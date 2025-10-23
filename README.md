## GitHub API Key Setup
This application requires a GitHub API key to make authenticated requests to the GitHub API when fetching repository data. This is necessary to avoid rate limiting when downloading repository information, forks, and commits.

### Why You Need This
Without authentication, GitHub limits API requests to 60 per hour. With a personal access token, this increases to 5,000 requests per hour.

### Cached Data
**Important:** This project includes pre-cached data in JSON format (except for cloned repositories) to save time and API calls. The application will use this cached data by default.

If you want to test the API calls yourself or fetch fresh data:
1. Delete the JSON cache files from the project directory
2. The application will then make fresh API calls to GitHub
3. **Note:** Making API calls from scratch will take a significant amount of time to complete, especially for repositories with many forks and commits

### Getting Your GitHub Token
1. Go to [GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)](https://github.com/settings/tokens)
2. Click "Generate new token" → "Generate new token (classic)"
3. Give your token a name
4. Select the **`public_repo`** scope
5. Click "Generate token"
6. **Copy the token immediately** - you won't be able to see it again!
7. Use this token in either Option 1 or Option 2 below

### Setup Instructions
#### Option 1: Environment Variable (Recommended)
Set the `GITHUB_API_KEY` environment variable:

**On macOS/Linux:**
```bash
export GITHUB_API_KEY=your_token_here
```

**On Windows (Command Prompt):**
```cmd
set GITHUB_API_KEY=your_token_here
```

**On Windows (PowerShell):**
```powershell
$env:GITHUB_API_KEY="your_token_here"
```

#### Option 2: Configuration File
1. Create a file named `config.properties` in the project root directory
2. Add the following line:
```properties
   github.api.key=your_token_here
```
