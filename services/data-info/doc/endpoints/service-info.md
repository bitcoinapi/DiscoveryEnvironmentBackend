# Service Information

    /status

This resource provides the name of the service and its version.

## Request

### Method

    GET

## Response

Only the responses with custom bodies are documented.

### OK (200)

#### Content Type

    application/json

#### Schema

    ServiceInfo {
      service (string):     the name of the service,
      description (string): a human-readable description of the service,
      version (string):     the version identifier of the service
    }

## Example

    ? curl localhost:31360 | python -mjson.tool
    {
        "description": "DE service for data information logic",
        "service": "data-info",
        "version": "4.0.4"
    }
