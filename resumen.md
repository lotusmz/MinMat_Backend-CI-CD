# Resumen SonarQube

**Proyecto:** '"$PROJECT_KEY"'

## Quality Gate
$(jq -r '.projectStatus.status' quality_gate.json)

## Métricas
$(jq -r '.component.measures[] | "\(.metric): \(.value)"' measures.json | sed 's/^/- /')

## Issues (por tipo)
$(jq -r '.facets[] | select(.property=="types") | .values[] | "- \(.val): \(.count)"' issues.json)

## Severidades
$(jq -r '.facets[] | select(.property=="severities") | .values[] | "- \(.val): \(.count)"' issues.json)

## Security Hotspots pendientes
$(jq -r '.paging.total' hotspots.json) pendientes de revisión
