# GitLab CI/CD Setup Guide

## 📋 Prerequisites

Before running the GitLab CI pipeline, you need to configure the following:

## 1. SonarQube Setup (Self-Hosted)

### Configure your SonarQube project:

1. Access your SonarQube server
2. Create a new project or use existing one
3. Generate an authentication token:
   - Go to **User > My Account > Security**
   - Generate a new token for CI/CD

### Update `sonar-project.properties`:

```properties
# Update with your SonarQube server URL
sonar.host.url=https://your-sonarqube-server.com

# Project key (already configured)
sonar.projectKey=t-aia-911_mobile_e86fd24e-e675-44ed-9e3f-5826a56a6d1c
```

Replace `https://your-sonarqube-server.com` with your actual SonarQube instance URL.

## 2. GitLab CI/CD Variables

Add the following variables to your GitLab project:

### Go to: Settings > CI/CD > Variables

Add these variables:

1. **`SONAR_TOKEN`**
   - Type: Variable
   - Value: Your SonarQube authentication token
   - Protected: ✅ Yes
   - Masked: ✅ Yes

2. **`SONAR_HOST_URL`**
   - Type: Variable
   - Value: Your SonarQube server URL (e.g., `https://sonarqube.your-domain.com`)
   - Protected: ✅ Yes
   - Masked: ❌ No

3. **`SONAR_PROJECT_KEY`**
   - Type: Variable
   - Value: `t-aia-911_mobile_e86fd24e-e675-44ed-9e3f-5826a56a6d1c`
   - Protected: ✅ Yes
   - Masked: ❌ No

4. **`EXPO_TOKEN`** (optional - for EAS builds)
   - Type: Variable
   - Value: Your Expo access token
   - Protected: ✅ Yes
   - Masked: ✅ Yes
   - Get it from: https://expo.dev/accounts/[account]/settings/access-tokens

## 3. GitLab Runner Configuration

### For basic pipeline (test, quality, build):
- Any Linux runner with Docker executor
- Node.js 20 image

### For Android builds:
- Runner with Docker executor
- `reactnativecommunity/react-native-android` image

### For iOS builds:
- macOS runner with shell executor
- Xcode installed
- Tag the runner with `macos`

## 4. Pipeline Stages

The `.gitlab-ci.yml` defines the following stages:

### 1. **Test Stage**
- Runs TypeScript type checking
- Runs Jest tests with coverage **once**
- Generates coverage reports
- Uploads coverage as artifacts (shared with SonarQube)
- Runs on: MR, main, develop branches

### 2. **Lint Stage** (optional)
- Runs ESLint
- Allowed to fail
- Runs on: MR, main, develop branches

### 3. **Quality Stage** (optimized)
- Runs SonarQube analysis
- **Reuses coverage from Test stage** (no tests re-run)
- Sends coverage to SonarQube
- Runs on: MR, main, develop branches

### 4. **Build Stage**
- Builds Expo bundle for all platforms
- Runs on: main, develop branches

### 5. **Build Android/iOS** (manual)
- Manual trigger only
- Runs on tags
- Creates native builds

## 5. Running the Pipeline

The CI will automatically run on:
- Merge requests
- Push to `main` or `develop` branches
- Manual trigger for native builds (tags only)

## 6. Local Testing

Before pushing, test locally:

```bash
# Run tests
npm test

# Run tests with coverage
npm run test:coverage

# Type check
npm run type-check

# Lint
npm run lint
```

## 7. Coverage Reports

GitLab will automatically display coverage in:
- Merge Request widget
- Pipeline view
- Project badges

### Enable coverage badge:

Go to **Settings > CI/CD > General pipelines** and add:
```
/All files[^|]*\|[^|]*\s+([\d\.]+)/
```

## 8. Badges (Optional)

Add status badges to your README.md:

```markdown
[![pipeline status](https://gitlab.com/your-org/mobile-app/badges/main/pipeline.svg)](https://gitlab.com/your-org/mobile-app/-/commits/main)
[![coverage report](https://gitlab.com/your-org/mobile-app/badges/main/coverage.svg)](https://gitlab.com/your-org/mobile-app/-/commits/main)
[![Quality Gate Status](https://your-sonarqube-server.com/api/project_badges/measure?project=t-aia-911_mobile_e86fd24e-e675-44ed-9e3f-5826a56a6d1c&metric=alert_status)](https://your-sonarqube-server.com/dashboard?id=t-aia-911_mobile_e86fd24e-e675-44ed-9e3f-5826a56a6d1c)
```

## 📝 Notes

- The CI uses `--legacy-peer-deps` due to React Native peer dependency issues
- **Tests run only once**: Coverage is generated in Test stage and shared with SonarQube via artifacts
- Coverage reports are uploaded to SonarQube
- TypeScript checking runs before tests
- Node modules are cached between pipeline runs
- **SonarQube connection**: Make sure your GitLab Runner can access your SonarQube server (network/firewall rules)
- **Performance**: SonarQube job uses `needs` with artifacts to start as soon as tests complete (parallel execution)

## 🔧 Troubleshooting

### Pipeline fails at install step:
```bash
# Clear cache and retry
# Go to CI/CD > Pipelines > Clear runner caches
```

### SonarQube fails:
- Check that `SONAR_TOKEN` is set correctly in GitLab CI/CD variables
- Check that `SONAR_HOST_URL` is set correctly
- Check that `SONAR_PROJECT_KEY` is set correctly
- Verify `sonar-project.properties` has correct `sonar.host.url`
- Ensure your GitLab Runner can access your SonarQube server
- Check SonarQube server logs for connection issues

### Tests fail:
- Run tests locally first: `npm test`
- Check test output in pipeline logs
- Verify all dependencies are installed

### Build fails:
- Ensure all dependencies are in `package.json`
- Check that TypeScript compiles: `npm run type-check`
- Check pipeline logs for specific errors

### Coverage not showing:
- Verify coverage regex in **Settings > CI/CD > General pipelines**
- Check that tests generate `coverage/` directory
- Verify `coverage_report` artifact is created

## 🚀 Advanced Configuration

### Parallel jobs:
You can run tests in parallel by splitting them:

```yaml
test:unit:
  stage: test
  script:
    - npm test -- --testPathPattern="src/.*\.test\.(ts|tsx)$"

test:integration:
  stage: test
  script:
    - npm test -- --testPathPattern="integration/.*\.test\.(ts|tsx)$"
```

### Deploy to EAS:
```yaml
deploy:eas:
  stage: deploy
  script:
    - npm ci --legacy-peer-deps
    - npx eas build --platform all --non-interactive
  only:
    - tags
  when: manual
```

### Scheduled pipelines:
Create a scheduled pipeline for nightly builds:
- Go to **CI/CD > Schedules**
- Create a new schedule
- Set interval (e.g., daily at 2 AM)
- Add variable: `NIGHTLY_BUILD=true`
