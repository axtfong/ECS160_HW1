# HW2 Verification Checklist

## ✅ Part A: Redis Persistence Framework

### Annotations
- [x] `@PersistableObject` - Class-level annotation ✓
- [x] `@PersistableField` - Field-level annotation ✓
- [x] `@Id` - ID field annotation ✓
- [x] `@LazyLoad(field="[FIELDNAME]")` - Method-level annotation for lazy loading (Extra Credit) ✓

### RedisDB Class
- [x] `bool persist(Object o)` - Persists objects with @PersistableField annotations ✓
- [x] `Object load(Object o)` - Loads objects by ID ✓
- [x] Handles nested objects (child objects) ✓
- [x] Handles `java.util.List` collections ✓
- [x] Lazy loading support (Extra Credit) ✓

### Build Status
- [x] Built and installed to local Maven repository ✓
- [x] `mvn clean install` works ✓

## ✅ Part B: Microservice Framework

### Annotations
- [x] `@Microservice` - Class-level annotation ✓
- [x] `@Endpoint(url = "[url]")` - Method-level annotation ✓

### MicroserviceLauncher
- [x] `bool launch(int port)` - Launches HTTP server ✓
- [x] Routes requests to correct microservice endpoints ✓
- [x] Handles GET requests ✓
- [x] Method signature validation: `String handleRequest(String input)` ✓

### Build Status
- [x] Built and installed to local Maven repository ✓
- [x] `mvn clean install` works ✓

## ✅ Part C: Microservices

### Microservice A: Issue Summarizer
- [x] `@Endpoint(url = "summarize_issue")` ✓
- [x] Accepts JSON of GitHub issue ✓
- [x] Returns Issue summary in JSON format ✓
- [x] Uses Ollama deepcoder:1.5b model ✓

### Microservice B: Bug Finder
- [x] `@Endpoint(url = "find_bugs")` ✓
- [x] Accepts C file contents ✓
- [x] Returns list of JSON Issues ✓
- [x] Uses Ollama deepcoder:1.5b model ✓

### Microservice C: Issue Comparator
- [x] `@Endpoint(url = "check_equivalence")` ✓
- [x] Accepts two lists of Issues in JSON format ✓
- [x] Returns JSON list of common Issues ✓
- [x] Uses Ollama deepcoder:1.5b model ✓

### Issue JSON Format
- [x] `bug_type`: String ✓
- [x] `line`: Integer ✓
- [x] `description`: String ✓
- [x] `filename`: String ✓

## ✅ Part D: Java Application

### Requirements
- [x] Loads repo from Redis using persistence framework ✓
- [x] Loads issues from Redis using persistence framework ✓
- [x] Clones repository using git ✓
- [x] Reads `selected_repo.dat` file ✓
- [x] Invokes Microservice A to summarize issues (IssueList1) ✓
- [x] Invokes Microservice B to find bugs in C files (IssueList2) ✓
- [x] Invokes Microservice C to compare lists ✓
- [x] Prints common issues ✓
- [x] Generates `ANALYSIS.md` file ✓

### File Structure
- [x] `selected_repo.dat` exists with repo ID and file paths ✓
- [x] `ANALYSIS.md` is generated after running ✓

## ✅ Part E: Testing

### Unit Tests
- [x] JUnit tests provided ✓
- [x] `PersistenceFrameworkTest` - Tests persist/load functionality ✓
- [x] `MicroserviceTest` - Tests microservices (with Ollama availability checks) ✓
- [x] Tests clean up after themselves (no test data pollution) ✓

### Test Results
- [x] All tests pass ✓
- [x] `mvn test` runs successfully ✓

## ✅ Build and Script

### script.sh
- [x] Builds persistence framework ✓
- [x] Builds microservice framework ✓
- [x] Builds application ✓
- [x] Runs tests ✓
- [x] Does NOT run the application (correct behavior) ✓

### Maven Build
- [x] All modules compile successfully ✓
- [x] Dependencies are correctly configured ✓
- [x] Parent POM manages all modules ✓

## ✅ Additional Requirements

### Redis Data Format
- [x] Repo records have `Url` field ✓
- [x] Repo records have `Issues` field (comma-separated issue IDs) ✓
- [x] Issue records have `Description` field ✓
- [x] Issues stored in database 1, Repos in database 0 ✓

### Ollama Integration
- [x] Uses OllamaClient to interface with Ollama ✓
- [x] Uses deepcoder:1.5b model ✓
- [x] Handles Ollama unavailability gracefully ✓

### Error Handling
- [x] Port conflict handling (8080) ✓
- [x] Repository not found handling ✓
- [x] Lists available repositories when repo not found ✓
- [x] Test data cleanup ✓

## Notes

1. **Redis Data**: Make sure HW1 has been run to populate Redis with repository and issue data in the correct format.

2. **Ollama**: The application requires Ollama to be running with the deepcoder:1.5b model installed. Tests handle Ollama unavailability gracefully.

3. **Port 8080**: The microservice server runs on port 8080. If the port is in use, the application will provide instructions to kill the process.

4. **selected_repo.dat**: Should contain:
   - First line: Repository ID (e.g., "repo-1738364184567")
   - Subsequent lines: Paths to C files to analyze

5. **ANALYSIS.md**: Generated automatically after running the application.

## Running the Application

1. Ensure Redis is running: `redis-server`
2. Ensure Ollama is running with deepcoder:1.5b model
3. Run HW1 to populate Redis (if not already done)
4. Update `selected_repo.dat` with a valid repository ID
5. Run: `cd application && mvn exec:java`

## Submission Checklist

- [x] Run `mvn clean` in all modules
- [ ] Delete cloned repository directories (cloned_repos_hw2)
- [ ] Include Redis `.db` file (if using file-based persistence)
- [x] Include `selected_repo.dat` file
- [x] Include `ANALYSIS.md` file (generated after running)
- [x] Zip the homework directory

