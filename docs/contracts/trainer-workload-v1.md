# Trainer Workload Event Contract Version 1

## Purpose

`gym-crm` publishes trainer workload changes and `gym-reports` consumes them to maintain monthly duration summaries in MongoDB.

## Transport

- Broker: ActiveMQ Classic
- Destination type: queue
- Queue: `trainer.workload`
- JMS message type: text JSON
- JMS type identifier property: `_type`
- JMS type identifier value: `trainer-workload-v1`
- Delivery expectation: at least once

Consumers must tolerate redelivery. `gym-reports` deduplicates events by `eventId` before changing a trainer summary.

## Common fields

| Field | Type | Required | Description |
|---|---|---:|---|
| `eventId` | UUID | Yes | Globally unique event identifier |
| `eventType` | String | Yes | `TRAINING_ADDED` or `TRAINER_STATUS_CHANGED` |
| `trainerUsername` | String | Yes | Stable trainer identifier |
| `trainerFirstName` | String | Yes | Current first name |
| `trainerLastName` | String | Yes | Current last name |
| `trainerStatus` | String | Yes | `ACTIVE` or `INACTIVE` |
| `trainingDate` | ISO date | Conditional | Required for `TRAINING_ADDED` |
| `trainingDuration` | Integer | Conditional | Positive minutes; required for `TRAINING_ADDED` |

Passwords, addresses, dates of birth and trainee information must not be included.

## TRAINING_ADDED

```json
{
  "eventId": "dd515481-70bc-4daf-88b0-8a930f9bef18",
  "eventType": "TRAINING_ADDED",
  "trainerUsername": "john.smith",
  "trainerFirstName": "John",
  "trainerLastName": "Smith",
  "trainerStatus": "ACTIVE",
  "trainingDate": "2026-10-20",
  "trainingDuration": 60
}
```

The consumer adds the duration to the matching trainer, calendar year and calendar month.

## TRAINER_STATUS_CHANGED

```json
{
  "eventId": "6761fc8d-e698-46bc-a6dd-1ff85cd365e4",
  "eventType": "TRAINER_STATUS_CHANGED",
  "trainerUsername": "john.smith",
  "trainerFirstName": "John",
  "trainerLastName": "Smith",
  "trainerStatus": "INACTIVE",
  "trainingDate": null,
  "trainingDuration": null
}
```

The consumer updates trainer identity and status without changing duration totals.

## Idempotency

Each trainer document contains the identifiers already applied to that summary. A redelivered `eventId` is acknowledged without applying the change again.

This MVP keeps processed identifiers without expiration. A production system can move them to an Inbox collection with a unique identifier and a retention policy, provided the retention window is longer than the maximum supported redelivery or replay period.

## Compatibility

Changes that add optional fields are backward compatible. Renaming fields, changing their types or adding required fields requires a new contract version and JMS type identifier.
