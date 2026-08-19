@echo off
echo ========================================================
echo Pushing product-service to feature/product-service branch
echo ========================================================
cd /d "%~dp0"

git config user.name "sanuda9988"
git config user.email "sanudasandeepa944@gmail.com"

echo.
echo Staging files...
git add .

echo.
echo Committing files...
git commit -m "Add Product Service implementation"

echo.
echo Pushing to GitHub repository...
git push -u origin feature/product-service

echo.
echo Done!
pause
