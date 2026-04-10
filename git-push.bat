@echo off
echo ====================================
echo Git Cleanup and Push Commands
echo ====================================
echo.
echo Run these commands in your terminal:
echo.
echo Step 1: Remove IDE files from Git tracking
git rm -r --cached .idea
git rm -r --cached .stakpak
git rm -r --cached .qwen 2>nul
echo.
echo Step 2: Verify .gitignore is updated (should already be correct)
git status
echo.
echo Step 3: Add all changes
git add .
echo.
echo Step 4: Review changes
git diff --staged
echo.
echo Step 5: Commit with message
git commit -m "docs: Update README with full tech stack and add comprehensive API documentation

- Updated README.md with complete technology stack in English
- Added docs/API_DOCUMENTATION.md with detailed method reference
- Enhanced project documentation with architecture diagrams
- Cleaned up Git tracking for IDE files (.idea, .stakpak, .qwen)
- All development environment files now excluded from version control

Features documented:
- REST API endpoints
- Kafka consumers and topics
- Email, Telegram, and Firebase Push services
- Redis caching for user lookup
- Scheduled weekly reports
- Database schema and entities

Documentation includes:
- Complete method signatures
- Request/response examples
- Error handling behavior
- Component dependency graphs
- Configuration reference"
echo.
echo Step 6: Verify commit
git status
echo.
echo Step 7: Push to GitHub
git push
echo.
echo ====================================
echo Done! Your changes are now pushed to GitHub
echo ====================================
pause
