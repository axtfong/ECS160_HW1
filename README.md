## GitHub API Key Setup

### About Cached Data
We've included pre-cached JSON data for the option of faster calls. To test API calls to fetch fresh data from GitHub:
1. Delete the JSON files in the root folder
2. Delete the JSON files in the commits directory. **Not the commits directory itself**

### Getting Your GitHub Token
1. Go to [GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)](https://github.com/settings/tokens)
2. Click "Generate new token" → "Generate new token (classic)"
3. Give your token a name
4. Select the `public_repo` scope
5. Click "Generate token"
6. Copy the token
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
