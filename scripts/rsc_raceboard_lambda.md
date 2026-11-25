A-# RaceBoard redirect: AWS Lambda behind an ALB

`rsc_raceboard_lambda.py` wraps the resolver in `rsc_raceboard.py` as an
Application Load Balancer (ALB) Lambda target. Both files use only Python's
standard library; no pip packages, layers or AWS SDK calls are needed.

## HTTP interface

```text
GET /rsc-raceboard?server=www.sapsailing.com&eventId=00502e4a-1c08-455b-8e22-280840638a39&rsc=SALW49ERFX------------PREL000200--
```

The three required, case-sensitive query parameter names are:

| Parameter | Value |
| --- | --- |
| `server` | Sailing Analytics domain, e.g. `www.sapsailing.com`. No `https://`, port or path. The Lambda queries it over HTTPS. |
| `eventId` | Sailing Analytics event UUID. |
| `rsc` | Exact, case-sensitive SwissTiming RSC / TracTrac `ExternalId`. |

The handler returns:

- **302 Found** with `Location: <resolved RaceBoard URL>` on success. Set the
  Lambda environment variable `REDIRECT_STATUS=301` for **301 Moved Permanently**.
- **404 Not Found** with a plain-text `Not found` body for missing/invalid
  parameters, missing or ambiguous matches, and upstream lookup failures.
  Diagnostic details are logged to CloudWatch.
- `HEAD` works like `GET` with an empty response body. Other methods return 404.

Responses include `Cache-Control: no-store`, so failed lookups and mappings that
may change while races are being attached are not cached. 302 is the default for
this evolving mapping. The handler supports both ALB header formats and decodes
URL-encoded query names/values exactly once. With multi-value headers enabled,
duplicate parameters are rejected; with the default ALB format, AWS supplies only
the last value. The ALB listener rule chooses the endpoint path; the handler does
not hard-code it.

The lookup uses the existing resolver's event-scoped UUID matching and TracTrac
feed discovery. The event must expose a discoverable TracTrac event-page URL in
`trackingConnectorInfos`; events without it return 404. This three-parameter
endpoint has no explicit-feed or leaderboard-filter parameters. Multiple viewing
contexts return 404 rather than selecting one arbitrarily. See
[the resolver documentation](rsc_raceboard.md) for those details.

The redirect honors the event's `baseURL`, falling back to `https://<server>`.
Consequently, querying `www.sapsailing.com` for Paris 2024 can redirect to
`paris2024.sailing.omegatiming.com`.

## Installation (AWS Console)

### 1. Build the deployment ZIP

From the repository root:

```sh
python3 -B -m unittest discover -s scripts -p 'test_rsc_raceboard_lambda.py' -v
zip -j /tmp/opencode/rsc-raceboard-lambda.zip \
  scripts/rsc_raceboard.py scripts/rsc_raceboard_lambda.py
unzip -l /tmp/opencode/rsc-raceboard-lambda.zip
```

Create the output directory first if using these instructions on another machine.
The ZIP must contain these two files **at its root**, with no `scripts/` prefix:

```text
rsc_raceboard.py
rsc_raceboard_lambda.py
```

### 2. Create the Lambda function

1. Select the AWS **account and Region containing your ALB**. The Lambda and its
   target group must be in the same account and Region.
2. Open **Lambda → Functions → Create function → Author from scratch**.
3. Name it `rsc-raceboard-redirect`.
4. Select the **Python 3.13** runtime. Both x86_64 and arm64 work.
5. Under **Permissions**, choose **Create a new role with basic Lambda
   permissions**. Its `AWSLambdaBasicExecutionRole` policy supplies CloudWatch
   logging; the code needs no additional AWS service permissions.
6. Create the function.

### 3. Upload and configure it

1. On **Code → Code source → Upload from → .zip file**, upload
   `rsc-raceboard-lambda.zip` and save.
2. On **Code → Runtime settings → Edit**, set **Handler** to:

   ```text
   rsc_raceboard_lambda.lambda_handler
   ```

3. On **Configuration → General configuration → Edit**, set:
   - **Memory:** 256 MB.
   - **Timeout:** 30 seconds. The default 3 seconds is too short for several
     upstream requests.
4. On **Configuration → Environment variables → Edit**, add:

   | Key | Example value / behavior |
   | --- | --- |
   | `ALLOWED_SERVERS` | `www.sapsailing.com,paris2024.sailing.omegatiming.com` — comma-separated exact domains accepted in the `server` parameter, matched case-insensitively. Defaults to `www.sapsailing.com`; add your other Sailing Analytics domains here. An empty list accepts no servers. |
   | `REDIRECT_STATUS` | `302` (default) or `301`. |

   `ALLOWED_SERVERS` applies to the queried Sailing Analytics domain, not the
   TracTrac feed domain or the event's configured RaceBoard destination.
5. Leave **Configuration → VPC** unconfigured for public Sailing Analytics
   servers. Lambda then has outbound internet access to Sailing Analytics and
   TracTrac. ALB invokes Lambda through the AWS service integration, so the
   function does not need to be attached to the ALB's VPC.

   If you do attach the function to your VPC, provide outbound HTTPS and DNS
   connectivity, normally through private subnets with a NAT gateway. Attaching
   Lambda to a public subnet alone does not give it internet access.

Each outbound request uses an 8-second socket timeout, shortened according to the
remaining Lambda execution time to reserve 2 seconds for generating a response.
This is not a hard wall-clock deadline for an entire lookup; Lambda's configured
timeout remains the ultimate limit.

### 4. Test in the Lambda console

On **Test**, create an event using this ALB-shaped payload:

```json
{
  "requestContext": {
    "elb": {
      "targetGroupArn": "arn:aws:elasticloadbalancing:eu-west-1:123456789012:targetgroup/rsc-raceboard/example"
    }
  },
  "httpMethod": "GET",
  "path": "/rsc-raceboard",
  "queryStringParameters": {
    "server": "www.sapsailing.com",
    "eventId": "00502e4a-1c08-455b-8e22-280840638a39",
    "rsc": "SALW49ERFX------------PREL000200--"
  },
  "headers": { "host": "redirect.example.com" },
  "body": "",
  "isBase64Encoded": false
}
```

The test ARN is just fixture data; no real target group is needed for this test.
Expect `statusCode: 302` and `headers.Location` containing a RaceBoard URL if the
live event still exposes this race. Replacing the RSC with `DOES-NOT-EXIST` should
produce `statusCode: 404`. A console test displays the returned JSON; it does not
itself follow the redirect.

### 5. Publish a version and create an alias

1. Choose **Actions → Publish new version** after uploading and configuring.
2. Open **Aliases → Create alias**.
3. Name it `live` and point it at the version just published.
4. Copy the alias ARN, ending with `:function:rsc-raceboard-redirect:live`.

The ALB will target this alias. Later you can deploy a new version by moving the
alias, without changing the ALB rule.

### 6. Create the Lambda target group

1. Open **EC2 → Load Balancing → Target Groups → Create target group**.
2. Select target type **Lambda function**.
3. Name the group `rsc-raceboard`.
4. Leave **health checks disabled** (the default for Lambda targets). A health
   check has no lookup parameters and would receive 404 from this handler.
5. Choose **Next**, select **Enter a Lambda function ARN**, and enter the `live`
   alias ARN from step 5. Create the target group.
6. On **Attributes → Edit**, enable **Multi value headers** and save. Both modes
   work; this one lets the handler detect repeated query parameters.

The console adds permission for Elastic Load Balancing to invoke Lambda. Verify
it under the alias/function's **Configuration → Permissions → Resource-based
policy**: the principal should be `elasticloadbalancing.amazonaws.com`, the action
`lambda:InvokeFunction`, and the source ARN this target group's ARN.

If you register through the CLI instead, first create the target group, then grant
permission **on the alias**, then register it (replace the example values):

```sh
REGION=eu-west-1
ACCOUNT_ID=123456789012
FUNCTION_NAME=rsc-raceboard-redirect
TARGET_GROUP_ARN='arn:aws:elasticloadbalancing:eu-west-1:123456789012:targetgroup/rsc-raceboard/REPLACE_ME'

aws lambda add-permission --region "$REGION" \
  --function-name "$FUNCTION_NAME" --qualifier live \
  --statement-id allow-rsc-alb \
  --principal elasticloadbalancing.amazonaws.com \
  --action lambda:InvokeFunction \
  --source-arn "$TARGET_GROUP_ARN" --source-account "$ACCOUNT_ID"

aws elbv2 register-targets --region "$REGION" \
  --target-group-arn "$TARGET_GROUP_ARN" \
  --targets "Id=arn:aws:lambda:$REGION:$ACCOUNT_ID:function:$FUNCTION_NAME:live"
```

This resource-based invocation permission is separate from the Lambda execution
role used for logging.

### 7. Route ALB requests to the target group

**For an existing ALB:**

1. Open **EC2 → Load Balancers → your ALB → Listeners and rules**.
2. Select the HTTPS listener (normally port 443), then **Manage rules → Add rule**.
3. Add a **Path** condition equal to `/rsc-raceboard`. If the ALB hosts multiple
   domains, also add a **Host header** condition for your redirect service domain,
   e.g. `redirect.example.com`.
4. Set the action to **Forward to target group**, selecting `rsc-raceboard`.
5. Give the rule an unused priority, evaluated before any broader matching rule
   (smaller numbers run first), and save.
6. Ensure the ALB's security group accepts client traffic on the listener port.
   Its idle timeout should exceed the Lambda timeout; the usual ALB default of
   60 seconds works with the 30-second Lambda timeout above.

**If you need a new ALB:**

1. In EC2, choose **Load Balancers → Create load balancer → Application Load
   Balancer**. Give it a name and select **Internet-facing**.
2. Select a VPC and public subnets in at least two Availability Zones, with an
   internet gateway route. Select/create a security group accepting HTTPS on 443
   from the intended clients (and HTTP on 80 if you add an HTTP listener).
3. Add an HTTPS listener on 443 and choose an **ACM certificate in the same
   Region** covering your redirect service domain. You can select the Lambda
   target group as the initial default action while creating the ALB.
4. Once created, add the path rule described above. For a dedicated ALB, set the
   default action to a fixed `404` response so other paths also return 404.
5. Add a Route 53 **A alias** record for `redirect.example.com` pointing to the
   ALB. With another DNS provider, use a CNAME for this subdomain.
6. If desired, add a port-80 listener whose action redirects to HTTPS, preserving
   the host, path and query string. For a quick HTTP-only test, a port-80 listener
   can instead forward directly to the Lambda target group.

No Lambda Function URL or API Gateway is needed. A **forward** action invokes the
Lambda; the Lambda's response supplies the dynamic HTTP redirect.

### 8. Test through the ALB

```sh
curl -i --get 'https://redirect.example.com/rsc-raceboard' \
  --data-urlencode 'server=www.sapsailing.com' \
  --data-urlencode 'eventId=00502e4a-1c08-455b-8e22-280840638a39' \
  --data-urlencode 'rsc=SALW49ERFX------------PREL000200--'
```

Expect `302` and a `Location` header. Open the same URL in a browser to follow the
redirect. Repeat with an unknown RSC and expect `404` without `Location`.
For an HTTP-only test, use `http://<ALB-DNS-name>/rsc-raceboard`; HTTPS testing
should use the domain covered by the listener certificate.

### 9. Deploy subsequent changes

Rebuild and upload the ZIP, configure `$LATEST` as needed, test it, publish a new
version, then edit alias `live` to point to that version. Uploading code or changing
environment variables on `$LATEST` alone does not change the published version
used by the ALB. Roll back by pointing `live` to the previous version.

## Troubleshooting

- **404 from Lambda:** inspect **Monitor → View CloudWatch logs**, under
  `/aws/lambda/rsc-raceboard-redirect`. Check the allowed domain, UUID, RSC,
  TracTrac discovery, and whether the race is attached in exactly one viewing
  context. Upstream HTTP, JSON and timeout errors also intentionally produce 404.
- **404 with no Lambda invocation:** check ALB rule conditions and priority.
- **502/503 or invocation denied:** check the target registration and alias-level
  invocation permission. The response header format must match the target group
  setting; this handler selects it from the incoming ALB event.
- **Timeout / 5xx:** check Lambda duration and outbound internet connectivity.
  A platform timeout, throttling, or invocation failure happens outside the
  handler and can produce an ALB 5xx rather than the handler's 404.
- **Import error:** both Python files must be at the ZIP root, and the handler
  setting must be `rsc_raceboard_lambda.lambda_handler`.

## AWS references

- [Lambda functions as ALB targets](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/lambda-functions.html)
- [Python ZIP deployment packages](https://docs.aws.amazon.com/lambda/latest/dg/python-package.html)
- [Internet access for VPC-connected Lambda functions](https://docs.aws.amazon.com/lambda/latest/dg/configuration-vpc-internet.html)
