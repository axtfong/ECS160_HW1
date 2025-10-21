## GitHub API Key Setup

This application requires a GitHub API key to make authenticated requests to the GitHub API when fetching repository data. This is necessary to avoid rate limiting when downloading repository information, forks, and commits.

To set up the GitHub API key:

1. Create a GitHub personal access token:
   - Go to GitHub Settings → Developer settings → Personal access tokens
   - Generate a new token with the `repo` scope
   - Copy the generated token

2. Set up the key using ONE of these methods:

   a) Environment Variable:
```
   export GITHUB_API_KEY=your_token_here
```

   b) Configuration File:
   - Create a file named `config.properties` in the project root directory (copy from config.properties.example)
   - Add the following line:
```
     github.api.key=your_token_here
```

The application is currently configured to use pre-downloaded JSON files, but with this API key setup, it would be ready to make direct API calls if needed.
```

### 3. Create or update `.gitignore` in project root
Add this line:
```
# Configuration with secrets
config.properties