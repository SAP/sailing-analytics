# Sailing Knowledge Agent

A Python prototype that lets users ask natural-language questions in for example Claude Code about sailing race performance and receive data-grounded, domain-aware answers — for example: *"Why did sailor XY perform well on Leg 2 of race XY?"*

The core idea is to bridge the gap between raw race telemetry, human raicing experience and sailing theory. This project explores whether an LLM-based system can turn that sailing analytics data into naturally sailing insight without the user needing to know anything about the underlying APIs.

### Different Approaches
1. Injecting the most important sailing facts into one SKILL.md and provide Claude
2. Embedding sailing theory and providing Claude through Plain RAG. By own script and by OneAI Tool
3. Embedding and Building Graph with Cognee on sailing theory and providing CLaude

In the end idea 2. with OneAI and 3. with Cognee were implemented and roughly tested on claude code sessions manually. 

### Insights
- only a prompt like "How did Sailor X perform in the Race Y? Use the docu: https://www.sapsailing.com/sailingserver/webservices/api/v1/index.html.": (1) struggling to get correct endpoints; needs 4-5 fetches before finding correct call structure (2) doesnt go into deepth of the analysis; stops fast (3) often understands sailing information wrong, for example the wind direction TO
- only the API Helper: (1) still lacking of depth of the analysis


###  Connected Links: 
- [Project Git Link](https://github.com/LisaSAP/sailing-knowledge-agent)
- OneAI Links: [OneAi ChatBot to try with sailing theory](https://oneai.cfapps.eu10-004.hana.ondemand.com/oneai/chatbot/application/index.html#/space/14cce80b-7c5f-48b8-9bb4-db72215656d4/public/13b5358f-20e1-4361-af76-1683952da347), [Github of OneAI](https://github.tools.sap/customer-engagement-workplace/oneai-spacemanager-api), [Doku](https://oneai.cfapps.eu10-004.hana.ondemand.com/oneai/docs/en/about.html)

## Architecture Overview

The project has two parts that work together toward Claude Code:

**Knowledge Graph Creation** — sailing literature (books, rule sets, tactics guides) is extracted, chunked, and ingested into a graph database using Cognee. The graph captures sailing concepts (maneuvers, wind patterns, tactics, rules) and their relationships. Claude Code queries this graph at question time to retrieve domain context. (see folder knowledgegraph)

**Claude Code Knowledge Enrichment** — a folder to start a new Claude Code Session. It contains two SKILLS, a Python wrappers around the public SAP Sailing REST API and two MCPs. They are adjustable through the .mcp.json and settings.json file. 
Claude Code acts as the orchestrator: it can call all the MCPs or Skills as tool, combines the context, and produces a natural-language answer.

## Repository Structure

```
sailing-knowledge-agent/
├── knowledgegraph/
│   ├── 0_ocr_extract.py       # PDF → JSONL chunks (Docling)
│   ├── 1_build_kg.py          # JSONL → Cognee knowledge graph
│   ├── 2_mcp_server.py        # MCP server exposing the KG as a tool
│   ├── Z_run_sailing.py       # End-to-end pipeline + test questions
│   ├── sailing_graph_prompt.md # LLM extraction instructions for the KG
│   ├── ontology.owl           # OWL ontology constraining KG extraction
│   └── source/                # Input PDFs and pre-chunked JSONL files
├── claudecode/
│   ├── sap_api.py             # SAP Sailing API wrappers
│   ├── .mcp.json              # MCP server registrations for Claude Code
│   └── .claude/
│       ├── settings.json      # Permissions and skill configuration
│       └── skills/            # Sailing domain skills (analysis, SAP API)
├── requirements.txt
└── .env.template
```

## Knowledge Graph Pipeline

### Step 1 — PDF extraction (`0_ocr_extract.py`)

Uses [Docling](https://github.com/DS4SD/docling) to extract text from sailing books and rule sets with structure preservation. Each document is split into chunks that retain page number and heading as metadata. Output is one JSONL file per PDF, written to `knowledgegraph/output/`.

```
# All PDFs in source/ — one JSONL each
python knowledgegraph/0_ocr_extract.py

# Specific file
python knowledgegraph/0_ocr_extract.py knowledgegraph/source/MyBook.pdf
```

Each JSONL line:
```json
{"chunk": 0, "pages": [3], "heading": "Tacking on Headers", "text": "..."}
```

### Step 2 — KG construction (`1_build_kg.py`)

Ingests JSONL chunks into [Cognee](https://github.com/topoteretes/cognee), which:

1. Embeds chunks into a vector store (LanceDB) for similarity search
2. Runs an LLM over each chunk guided by `sailing_graph_prompt.md` to extract typed nodes and edges
3. Optionally constrains extraction to `ontology.owl` via fuzzy class matching (threshold 0.6)

Node types extracted: `Maneuver`, `WindPattern`, `WindTactic`, `SailingMetric`, `RightOfWayRule`, `PositioningConcept`, `CourseManagement`, and others.

```
python knowledgegraph/1_build_kg.py \
  --jsonl knowledgegraph/source/EXAMPLE_Dave_Perry_Quiz_Sample.jsonl \
  --ontology knowledgegraph/ontology.owl
```

Output: graph stored in `knowledgegraph/.cognee_system/` + `knowledgegraph/output/graph.html` (interactive visualisation).

### Step 3 — MCP server (`2_mcp_server.py`)

Exposes the knowledge graph as a single MCP tool `search_kg(question)` that Claude Code can call. Uses Cognee's `GRAPH_COMPLETION` search: embed question → vector similarity → graph traversal → returns relevant concept strings.

Registered in `claudecode/.mcp.json` and enabled via `claudecode/.claude/settings.json`.

## SAP Sailing API Layer (`sap_api.py`)

Wraps the public SAP Sailing REST API. Key functions:

| Function | Returns | Auth |
|---|---|---|
| `search_regattas(query)` | Matching regattas by name | Public |
| `get_race_list(regatta)` | All races in a regatta | Public |
| `get_race_standings(regatta, race)` | Final/live rankings | Public |
| `get_competitor_legs(regatta, race)` | Per-leg stats (VMG, XTE, tacks) | Public |
| `get_mark_passings(regatta, race)` | Per-competitor mark times | Public |
| `get_start_analysis(regatta, race)` | Start quality metrics | Public |
| `get_maneuvers(regatta, race)` | Tack/gybe detail + distance loss | Public |
| `get_target_time(regatta, race)` | Wind per leg (no EXPORT needed) | Public |
| `get_competitor_data(...)` | Time-series VMG, rank, gap | Premium |
| `get_wind_data(regatta, race)` | Raw wind time-series | EXPORT |

Supports per-event subdomains (e.g. `austrianleague2026.sapsailing.com`) via `resolve_base_url()`.

## Usage

### Setup

```bash
cd sailing-knowledge-agent
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.template .env   # fill in LLM_ENDPOINT, LLM_API_KEY, LLM_MODEL, EMBEDDING_*
```

### Build the knowledge graph (optional)

```bash
# Extract PDFs
python knowledgegraph/0_ocr_extract.py

# Build KG from extracted chunks
python knowledgegraph/1_build_kg.py \
  --jsonl knowledgegraph/output/*.jsonl \
  --ontology knowledgegraph/ontology.owl
```

### Start Claude Code

```bash
cd sailing-knowledge-agent/claudecode
source ../.venv/bin/activate
claude
```

Claude Code will load the MCP server (`sailing-kg`), the SAP API skill, and the sailing analysis skill automatically.

For the different Testing Version adjust settings.json:

- Version0_WithNone: all MCP moving to "disabledMcpjsonServers", Skills turned "off"

- Version1_WithAPI: all MCP moving to "disabledMcpjsonServers", only Skill sailing-analysis turned "off"

- Version2_WithDomain: Only MCP "sailing-kg" under "disabledMcpjsonServers", only Skill sailing-sap-api turned "off"

- Version3_WithAll: As it is. 

- Version4_WithDomain_KG: Only MCP ""oneai-chat" under "disabledMcpjsonServers".

## Configuration
Parameters related to Claude Code session are set in `claudecode/.claude`
| Variable | Purpose |
|---|---|
| "permissions" | Allowing Skill and MCP calls |
| "enabledMcpjsonServers" | Allowing MCP |
| "claudeMdExcludes" | Excluding CLAUDE.md file |
| "skillOverrides" | Allowing Skills |
| "autoMemory" | Disable autoMemory |

Parameters related to Models are set in `.env` (based on `.env.template`):

| Variable | Purpose | Required |
|---|---|---|
| `LLM_ENDPOINT` | OpenAI-compatible LLM endpoint | Yes |
| `LLM_API_KEY` | LLM API key | Yes |
| `LLM_MODEL` | Model name (litellm string) | Yes |
| `EMBEDDING_ENDPOINT` | Embedding endpoint | Yes |
| `EMBEDDING_API_KEY` | Embedding API key | Yes |
| `EMBEDDING_MODEL` | Embedding model name | Yes |
| `EMBEDDING_DIMENSIONS` | Vector dimensions | Yes |
| `SAP_SAILING_USERNAME` | Premium API credentials | Optional |
| `SAP_SAILING_PASSWORD` | Premium API credentials | Optional |
| `VLM_MODEL` | Vision model for `--describe-pictures` | Optional |



## Key Design Decisions

**Knowledge graph or plain RAG?** PRO KG: A graph captures connections explicitly and allows traversal-based retrieval that flat vector search misses. CON: It requires high maintenance and is more complex to understand. Especially where each information comes from. 

**Why Cognee?** It handles the full pipeline — chunking, embedding, LLM-guided entity extraction, graph storage, and hybrid search — in a small dependency footprint. The `custom_prompt` and ontology parameters give control over what gets extracted without building a custom pipeline.

**Why separate KG and SAP API?** The idea is that KG holds timeless sailing domain knowledge (theory, rules, tactics). The SAP API provides race-specific facts (who rounded what mark when). The LLM reasons over both to answer questions that require both domain context and race data.

## Open Tasks
- **real test metric**: A measurable metric is missing to say a Skill or MCP is improving the question
- **tracking the costs**: No detailed insights were collected how expensive the QA were and what version costs more/less
- **observability**: Oftentimes the answere were partilly correct or it was obvious were the LLM "went wrong". A good tooling is missing to track the process of calculating. 
- **imroving API Enrichment further**: There are still obvious enhancments of the API Skill for example the correct understanding of the information "direction" in wind, due to the wind TO answer, and not FROM answer as the LLM mostly understand
- **more Endpoints**: No DataMining Endpoint is yet available. That is way the LLM often calculate numbers themself.
