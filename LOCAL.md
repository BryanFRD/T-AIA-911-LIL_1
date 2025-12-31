# Local Setup for MLflow with S3 Storage

This guide will help you set up your local environment for MLflow with S3 storage.

## 1. Install Python Packages

Python 3.10+ is required. Install the necessary packages using pip:

````bash
# Windows
pip install mlflow==3.8.1 boto3
````

# macOS / Linux
````
pip3 install mlflow==3.8.1 boto3
````

:warning: MacOS users with Python installed via Homebrew might need to add the `--break-system-packages` flag:
````bash
# MacOS
python3 -m pip install mlflow==3.8.1 boto3 --break-system-packages
````

## 2. Create .env File

Create a file named `.env` in the root directory with the following content:

````
MLFLOW_TRACKING_URI=
MLFLOW_TRACKING_INSECURE_TLS=
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
MLFLOW_S3_ENDPOINT_URL=
MLFLOW_S3_IGNORE_TLS=
````

## 3. IntelliJ Configuration

Spring boot dont load env file by default. To load it, follow these steps:

1. Install the **EnvFile** plugin from the IntelliJ marketplace
2. Go to **Run** > **Edit Configurations...**
3. Enable the **EnvFile** checkbox
4. Click on the **+** icon and select your `.env` file


Enjoy :)