# Use lightweight Python image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install dependencies
COPY requirments.txt .
RUN pip install --no-cache-dir -r requirments.txt

# Copy app code
COPY app.py .

# Create storage directory inside container
RUN mkdir -p /app/storage

# Expose Flask port
EXPOSE 5000

# Run the app
CMD ["python", "app.py"]
