# Security Specification (Phase 0 TDD & Red Team Audit)

## 1. Data Invariants
1. **Tickets (`/tickets/{ticketId}`)**:
   - Every ticket belongs strictly to a single authenticated user (`userId == request.auth.uid`).
   - Unauthenticated users cannot read, list, create, update, or delete any ticket.
   - `list` queries MUST enforce `resource.data.userId == request.auth.uid`.
   - `userId`, `orderId`, `bookingReference`, and `createdAt` are immutable upon update.
   - Only `addedToGoogleWallet` and `updatedAt` may be mutated on update.
2. **Markup Configurations (`/markup_configs/{userId}`)**:
   - Document ID `{userId}` MUST equal `request.auth.uid` and `incoming().userId == request.auth.uid`.
   - Numeric margins (`fixedMarkupEur`, `percentageMarkup`, ancillary fees) must be non-negative numbers within sane upper bounds (`<= 5000`).

## 2. The Dirty Dozen Payloads
1. Unauthenticated read on `/tickets/t1` -> Reject
2. Cross-user read by Bob on Alice's `/tickets/t1` -> Reject
3. Unfiltered list on `/tickets` without `where("userId", "==", uid)` -> Reject
4. Spoofed `userId` on ticket creation (`userId: "other_uid"`) -> Reject
5. Ghost field injection (`isVipFree: true`) on `/tickets/t1` create -> Reject (`hasOnly`)
6. Oversized string (`passengerName` > 120 chars) -> Reject
7. Invalid `cabinClass` enum (`"supersonic_luxury"`) -> Reject
8. Negative `totalChargedEur` (`-50.0`) -> Reject
9. Mutating immutable `bookingReference` or `totalChargedEur` on ticket update -> Reject (`affectedKeys().hasOnly(['addedToGoogleWallet', 'updatedAt'])`)
10. Future timestamp injection (`createdAt > request.time`) -> Reject (`<= request.time`)
11. Cross-user write on `/markup_configs/{bobUid}` by Alice -> Reject
12. Negative percentage markup (`percentageMarkup: -15`) on `/markup_configs/{uid}` -> Reject
