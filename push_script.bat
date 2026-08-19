@echo off
set GIT_TERMINAL_PROMPT=0
cd /d "c:\Users\PC\Desktop\product-service"
git config user.name "sanuda9988"
git config user.email "sanudasandeepa944@gmail.com"
git -c credential.helper= add . > push_result.txt 2>&1
git -c credential.helper= commit -m "Add Product Service implementation" >> push_result.txt 2>&1
git -c credential.helper= push -u origin feature/product-service >> push_result.txt 2>&1
