We want all local variables, fields, and parameters to be declared "final" as far as possible.

We want all literals used as parameters (true, false, numeric literals, Optional.empty() and null) to be prefixed with a comment telling the parameter name.

We want a single exit point in any method only, so no multiple "return" statements.

Avoid unnecessary empty lines. If you feel like you need to separate code within a single method into "sections", consider using double-slash comments to explain what the next section is.
