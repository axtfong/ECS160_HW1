## GitHub API Key Setup

This application requires a GitHub API key to make authenticated requests to the GitHub API when fetching repository data. This is necessary to avoid rate limiting when downloading repository information, forks, and commits.

To set up the GitHub API key:

1. Create a GitHub personal access token:
   - Go to GitHub Settings → Developer settings → Personal access tokens
   - Generate a new token with the `repo` scope
   - Copy the generated token

2. Set up the key.
   - Create a file named `config.properties` in the project root directory
   - Add the following line:
   ```
   github.api.key=your_token_here
   ```
