# ChargingPoint Status - Developer Guide

## Valid Status Values

```java
public enum ChargingPointStatus {
    AVAILABLE,      // Ready for use
    CHARGING,       // Currently charging a vehicle
    OUT_OF_SERVICE, // Broken/unavailable
    MAINTENANCE     // Under maintenance
}
```

⚠️ **IMPORTANT:** `OCCUPIED` was removed on 2025-10-30 (it was never used and caused bugs)

---

## Status Lifecycle

### Normal Flow
```
┌──────────────┐
│  AVAILABLE   │ ← Initial state
└──────┬───────┘
       │ Session starts
       ↓
┌──────────────┐
│  CHARGING    │ ← Active session
└──────┬───────┘
       │ Session ends
       ↓
┌──────────────┐
│  AVAILABLE   │ ← Ready again
└──────────────┘
```

### Maintenance Flow
```
AVAILABLE → MAINTENANCE → AVAILABLE
```

### Service Issue
```
AVAILABLE → OUT_OF_SERVICE → AVAILABLE (after repair)
```

---

## When to Set Each Status

### `AVAILABLE`
**When:**
- Initial setup
- After session completes
- After maintenance completes
- After repair completes

**Code:**
```java
chargingPoint.setStatus(ChargingPointStatus.AVAILABLE);
chargingPoint.setCurrentSession(null);
```

### `CHARGING`
**When:**
- Session starts (status = IN_PROGRESS)

**Code:**
```java
chargingPoint.setStatus(ChargingPointStatus.CHARGING);
chargingPoint.setCurrentSession(session);
```

### `OUT_OF_SERVICE`
**When:**
- Hardware failure
- Connection issues
- Safety concerns

**Code:**
```java
chargingPoint.setStatus(ChargingPointStatus.OUT_OF_SERVICE);
chargingPoint.setCurrentSession(null); // Clear any session
```

### `MAINTENANCE`
**When:**
- Scheduled maintenance
- Upgrades
- Preventive service

**Code:**
```java
chargingPoint.setStatus(ChargingPointStatus.MAINTENANCE);
chargingPoint.setCurrentSession(null); // Clear any session
```

---

## Common Queries

### Count Available Points
```java
long available = chargingPointRepository.countByStatus(ChargingPointStatus.AVAILABLE);
```

### Count Active (Charging) Points
```java
long charging = chargingPointRepository.countByStatus(ChargingPointStatus.CHARGING);
```

### Find Available Points at Station
```java
List<ChargingPoint> availablePoints = chargingPointRepository
    .findByStation_StationIdAndStatus(stationId, ChargingPointStatus.AVAILABLE);
```

### Check if Point is Available
```java
if (chargingPoint.getStatus() != ChargingPointStatus.AVAILABLE) {
    throw new AppException(ErrorCode.CHARGING_POINT_NOT_AVAILABLE);
}
```

---

## Validation Rules

### Before Starting Session
```java
// ✅ Point must be AVAILABLE
if (chargingPoint.getStatus() != ChargingPointStatus.AVAILABLE) {
    throw new AppException(ErrorCode.CHARGING_POINT_NOT_AVAILABLE);
}

// ✅ Point should have no current session
if (chargingPoint.getCurrentSession() != null) {
    throw new AppException(ErrorCode.CHARGING_POINT_IN_USE);
}
```

### Before Deleting Point
```java
// ✅ Point should not be CHARGING
if (chargingPoint.getStatus() == ChargingPointStatus.CHARGING) {
    throw new AppException(ErrorCode.CHARGING_POINT_IN_USE);
}
```

---

## Statistics & Dashboards

### Active Points (Currently Charging)
```java
// ✅ CORRECT - Count CHARGING status
long activePoints = chargingPointRepository.countByStatus(ChargingPointStatus.CHARGING);

// ❌ WRONG - Don't count OCCUPIED (removed!)
// long activePoints = chargingPointRepository.countByStatus(ChargingPointStatus.OCCUPIED);
```

### Operational Points
```java
// Points that can charge (AVAILABLE + CHARGING)
long operational = chargingPointRepository.countByStatus(ChargingPointStatus.AVAILABLE)
                 + chargingPointRepository.countByStatus(ChargingPointStatus.CHARGING);
```

### Offline Points
```java
// Points not working
long offline = chargingPointRepository.countByStatus(ChargingPointStatus.OUT_OF_SERVICE)
             + chargingPointRepository.countByStatus(ChargingPointStatus.MAINTENANCE);
```

---

## Common Mistakes ❌

### ❌ DON'T: Use OCCUPIED
```java
// This was removed! Will cause compilation error
chargingPoint.setStatus(ChargingPointStatus.OCCUPIED); // ❌
```

### ❌ DON'T: Forget to clear currentSession
```java
// When ending session
chargingPoint.setStatus(ChargingPointStatus.AVAILABLE);
// Missing: chargingPoint.setCurrentSession(null); ❌
```

### ❌ DON'T: Set CHARGING without a session
```java
chargingPoint.setStatus(ChargingPointStatus.CHARGING);
// Missing: chargingPoint.setCurrentSession(session); ❌
```

---

## Best Practices ✅

### ✅ DO: Always update both status and currentSession together
```java
// Starting session
chargingPoint.setStatus(ChargingPointStatus.CHARGING);
chargingPoint.setCurrentSession(session);
chargingPointRepository.save(chargingPoint);

// Ending session
chargingPoint.setStatus(ChargingPointStatus.AVAILABLE);
chargingPoint.setCurrentSession(null);
chargingPointRepository.save(chargingPoint);
```

### ✅ DO: Use transactions
```java
@Transactional
public void startSession(...) {
    // Update session
    session.setStatus(ChargingSessionStatus.IN_PROGRESS);
    
    // Update point
    chargingPoint.setStatus(ChargingPointStatus.CHARGING);
    chargingPoint.setCurrentSession(session);
    
    // Both saved in same transaction
}
```

### ✅ DO: Validate before status change
```java
if (chargingPoint.getStatus() != ChargingPointStatus.AVAILABLE) {
    throw new AppException(ErrorCode.CHARGING_POINT_NOT_AVAILABLE);
}
chargingPoint.setStatus(ChargingPointStatus.CHARGING);
```

---

## Migration Notes

### If You See OCCUPIED in Old Code
- **Replace with:** `CHARGING`
- **Why:** OCCUPIED was removed (2025-10-30)
- **See:** `OCCUPIED_STATUS_REMOVAL.md` for details

### Database
- No migration needed (OCCUPIED was never actually stored)
- All existing data uses valid statuses

---

## Testing

### Unit Test Example
```java
@Test
void testPointStatusTransition() {
    ChargingPoint point = new ChargingPoint();
    point.setStatus(ChargingPointStatus.AVAILABLE);
    
    // Start session
    ChargingSession session = new ChargingSession();
    point.setStatus(ChargingPointStatus.CHARGING);
    point.setCurrentSession(session);
    
    assertEquals(ChargingPointStatus.CHARGING, point.getStatus());
    assertNotNull(point.getCurrentSession());
    
    // End session
    point.setStatus(ChargingPointStatus.AVAILABLE);
    point.setCurrentSession(null);
    
    assertEquals(ChargingPointStatus.AVAILABLE, point.getStatus());
    assertNull(point.getCurrentSession());
}
```

---

## FAQs

**Q: Why was OCCUPIED removed?**  
A: It was never used in code, causing dashboard bugs (always showed 0 active points).

**Q: What's the difference between CHARGING and OCCUPIED?**  
A: There isn't one anymore - we only use CHARGING now.

**Q: Can I set status to OCCUPIED for backward compatibility?**  
A: No, it's been removed from the enum. Use CHARGING instead.

**Q: Do I need to migrate my database?**  
A: No, OCCUPIED was never stored in the database.

---

## Related Files

- `ChargingPointStatus.java` - Enum definition
- `ChargingSessionService.java` - Session management
- `ChargingSimulatorService.java` - Status transitions
- `OCCUPIED_STATUS_REMOVAL.md` - Migration details

---

**Last Updated:** 2025-10-30  
**Version:** 2.0 (after OCCUPIED removal)

