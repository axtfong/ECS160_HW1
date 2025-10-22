## GitHub API Key Setup

This application requires a GitHub API key to make authenticated requests to the GitHub API when fetching repository data. This is necessary to avoid rate limiting when downloading repository information, forks, and commits.

### Why You Need This
Without authentication, GitHub limits API requests to 60 per hour. With a personal access token, this increases to 5,000 requests per hour.

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

### Getting Your GitHub Token

1. Go to [GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)](https://github.com/settings/tokens)
2. Click "Generate new token" → "Generate new token (classic)"
3. Give your token a descriptive name (e.g., "ECS160 Homework")
4. Select the **`public_repo`** scope (or `repo` if you need access to private repositories)
5. Click "Generate token"
6. **Copy the token immediately** - you won't be able to see it again!
7. Use this token in either Option 1 or Option 2 above

### Security Notes
- **Never commit your API key to version control**
- The `config.properties` file should be added to `.gitignore`
- Keep your token secure and don't share it publicly
- You can revoke and regenerate tokens at any time from GitHub settings
