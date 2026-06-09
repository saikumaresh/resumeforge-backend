# Phase 4: HTTP Status Code Fixes (Quick Wins)

**Estimated Time:** 2 hours  
**Priority:** HIGH (required for API compliance)  
**Files to Modify:** 5 controller endpoints

---

## Current Issues

All POST/DELETE operations return **200 OK** when they should return **201/202/204**.

---

## Fix List

### 1. POST /api/v1/auth/register
**Current:** 200 OK  
**Should Be:** 201 Created (resource created)

```java
// File: ResumeController.java (or AuthController.java)

// BEFORE
@PostMapping("/register")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.ok(response);  // 200 OK ❌
}

// AFTER
@PostMapping("/register")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);  // 201 Created ✅
}
```

---

### 2. POST /api/v1/resumes (Create Resume)
**Current:** 200 OK  
**Should Be:** 201 Created

```java
// BEFORE
@PostMapping
public ResponseEntity<MasterResumeResponse> createResume(
        @Valid @RequestBody CreateMasterResumeRequest request,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    MasterResumeResponse response = resumeService.create(extractUserId(token), request);
    return ResponseEntity.ok(response);  // 200 OK ❌
}

// AFTER
@PostMapping
public ResponseEntity<MasterResumeResponse> createResume(
        @Valid @RequestBody CreateMasterResumeRequest request,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    MasterResumeResponse response = resumeService.create(extractUserId(token), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);  // 201 Created ✅
}
```

---

### 3. POST /api/v1/resumes/tailor (Async Tailoring)
**Current:** 200 OK  
**Should Be:** 202 Accepted (async processing queued)

```java
// BEFORE
@PostMapping("/tailor")
public ResponseEntity<TailorResponse> tailorResume(
        @Valid @RequestBody TailorResumeRequest request,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    TailorResponse response = resumeService.tailorAsync(extractUserId(token), request);
    return ResponseEntity.ok(response);  // 200 OK ❌
}

// AFTER
@PostMapping("/tailor")
public ResponseEntity<TailorResponse> tailorResume(
        @Valid @RequestBody TailorResumeRequest request,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    TailorResponse response = resumeService.tailorAsync(extractUserId(token), request);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);  // 202 Accepted ✅
}
```

---

### 4. PUT /api/v1/resumes/{id} (Update Resume)
**Current:** 200 OK  
**Should Be:** 200 OK (already correct, but add Location header for best practice)

```java
// OPTIONAL: Add Location header for consistency
@PutMapping("/{id}")
public ResponseEntity<MasterResumeResponse> updateResume(
        @PathVariable UUID id,
        @Valid @RequestBody CreateMasterResumeRequest request,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    MasterResumeResponse response = resumeService.update(extractUserId(token), id, request);
    return ResponseEntity.ok()
            .location(URI.create("/api/v1/resumes/" + response.getId()))
            .body(response);  // 200 OK with Location header ✅
}
```

---

### 5. DELETE /api/v1/resumes/{id} (Delete Resume)
**Current:** 200 OK  
**Should Be:** 204 No Content (no response body)

```java
// BEFORE
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteResume(
        @PathVariable UUID id,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    resumeService.delete(extractUserId(token), id);
    return ResponseEntity.ok().build();  // 200 OK ❌
}

// AFTER
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteResume(
        @PathVariable UUID id,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    resumeService.delete(extractUserId(token), id);
    return ResponseEntity.noContent().build();  // 204 No Content ✅
}
```

---

### 6. POST /api/v1/jobs (Create Job Description)
**Current:** 200 OK  
**Should Be:** 201 Created

```java
// BEFORE
@PostMapping
public ResponseEntity<JobDescriptionResponse> createJob(
        @Valid @RequestBody CreateJobDescriptionRequest request,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    JobDescriptionResponse response = jobService.create(extractUserId(token), request);
    return ResponseEntity.ok(response);  // 200 OK ❌
}

// AFTER
@PostMapping
public ResponseEntity<JobDescriptionResponse> createJob(
        @Valid @RequestBody CreateJobDescriptionRequest request,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    JobDescriptionResponse response = jobService.create(extractUserId(token), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);  // 201 Created ✅
}
```

---

### 7. DELETE /api/v1/jobs/{id} (Delete Job)
**Current:** 200 OK  
**Should Be:** 204 No Content

```java
// BEFORE
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteJob(
        @PathVariable UUID id,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    jobService.delete(extractUserId(token), id);
    return ResponseEntity.ok().build();  // 200 OK ❌
}

// AFTER
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteJob(
        @PathVariable UUID id,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    jobService.delete(extractUserId(token), id);
    return ResponseEntity.noContent().build();  // 204 No Content ✅
}
```

---

### 8. Add @Valid to Request DTOs
**Issue:** No validation on POST/PUT bodies

```java
// Endpoints to update with @Valid:

// 1. AuthController.register
@PostMapping("/register")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    // ✅ Already has @Valid

// 2. AuthController.login
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    // ✅ Already has @Valid

// 3. ResumeController.createResume
@PostMapping
public ResponseEntity<MasterResumeResponse> createResume(
        @Valid @RequestBody CreateMasterResumeRequest request) {
    // ADD: @Valid if missing

// 4. ResumeController.tailorResume
@PostMapping("/tailor")
public ResponseEntity<TailorResponse> tailorResume(
        @Valid @RequestBody TailorResumeRequest request) {
    // ADD: @Valid if missing

// 5. JobController.createJob
@PostMapping
public ResponseEntity<JobDescriptionResponse> createJob(
        @Valid @RequestBody CreateJobDescriptionRequest request) {
    // ADD: @Valid if missing
```

---

## Validation Fix Template

If @Valid is missing, add it:

```java
// BEFORE
@PostMapping
public ResponseEntity<Response> create(@RequestBody Request request) {
    // No validation ❌
}

// AFTER
@PostMapping
public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
    // Spring validates automatically ✅
    // Invalid data returns 400 Bad Request
}
```

---

## HTTP Status Code Reference

| Code | Meaning | When to Use |
|------|---------|------------|
| **200** | OK | GET, successful query |
| **201** | Created | POST that creates a resource |
| **202** | Accepted | Async operation queued |
| **204** | No Content | DELETE, successful with no body |
| **400** | Bad Request | Invalid input (@Valid fails) |
| **401** | Unauthorized | Missing/invalid JWT |
| **403** | Forbidden | Authenticated but no permission (BOLA) |
| **404** | Not Found | Resource doesn't exist |
| **409** | Conflict | Duplicate email, already exists |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Internal Error | Unhandled exception |

---

## Testing After Fixes

```bash
# Test 201 Created
curl -X POST http://localhost:8080/api/v1/resumes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"My Resume","content":"..."}' \
  -w "\nHTTP Status: %{http_code}\n"
# Expected: 201

# Test 204 No Content  
curl -X DELETE http://localhost:8080/api/v1/resumes/$ID \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nHTTP Status: %{http_code}\n"
# Expected: 204

# Test 400 Bad Request (invalid data)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"invalid","password":""}' \
  -w "\nHTTP Status: %{http_code}\n"
# Expected: 400 (validation failed)
```

---

## Checklist

- [ ] Fix POST /auth/register → 201 Created
- [ ] Fix POST /resumes → 201 Created
- [ ] Fix POST /resumes/tailor → 202 Accepted
- [ ] Fix DELETE /resumes/{id} → 204 No Content
- [ ] Fix POST /jobs → 201 Created
- [ ] Fix DELETE /jobs/{id} → 204 No Content
- [ ] Add @Valid to all POST/PUT endpoints
- [ ] Test all endpoints with curl
- [ ] Verify status codes in RateLimitFilterTest expectations
- [ ] Update API documentation

---

**Time to Complete:** 2 hours  
**Impact:** API compliance, better client integration, clearer intent

Implement these fixes before submission.
