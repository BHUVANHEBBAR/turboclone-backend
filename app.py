from flask import Flask, request, jsonify, send_file, abort
import os
import uuid
import glob

app = Flask(__name__)

BASE_STORAGE = "storage"
@app.route("/", methods=["GET"])
def index():
    return jsonify({
        "service": "FlashBucket – DB-less File Storage API",
        "description": "Upload, list, download files and track storage usage",
        "endpoints": {
            "Upload file": {
                "method": "POST",
                "url": "/files/upload",
                "curl": "curl -X POST https://flashbucket.onrender.com/files/upload "
                        "-F \"userId=42\" -F \"file=@example.txt\""
            },
            "List all files (admin)": {
                "method": "GET",
                "url": "/files",
                "curl": "curl -X GET https://flashbucket.onrender.com/files"
            },
            "List files for a user": {
                "method": "GET",
                "url": "/files/<userId>",
                "example": "/files/42",
                "curl": "curl -X GET https://flashbucket.onrender.com/files/42"
            },
            "Download file": {
                "method": "GET",
                "url": "/<userId>/<fileId>",
                "example": "/42/53eadd53-3ce2-49d3-b60f-5263fd27a78e",
                "curl": "curl -O https://flashbucket.onrender.com/42/<fileId>"
            },
            "Storage usage (total)": {
                "method": "GET",
                "url": "/storage/usage",
                "curl": "curl -X GET https://flashbucket.onrender.com/storage/usage"
            },
            "Storage usage (per user)": {
                "method": "GET",
                "url": "/storage/usage/<userId>",
                "example": "/storage/usage/42",
                "curl": "curl -X GET https://flashbucket.onrender.com/storage/usage/42"
            }
        },
        "example_live_outputs": {
            "GET /storage/usage": {
                "bytes": 23,
                "usage": "23.00 B"
            },
            "GET /files": {
                "count": 2,
                "files": [
                    {
                        "userId": "42",
                        "fileId": "53eadd53-3ce2-49d3-b60f-5263fd27a78e",
                        "fileName": "test.pdf",
                        "url": "/42/53eadd53-3ce2-49d3-b60f-5263fd27a78e"
                    },
                    {
                        "userId": "42",
                        "fileId": "0a592181-01dd-4376-81f8-83a5e1e2abf1",
                        "fileName": "ninamman.txt",
                        "url": "/42/0a592181-01dd-4376-81f8-83a5e1e2abf1"
                    }
                ]
            }
        }
    })
os.makedirs(BASE_STORAGE, exist_ok=True)
def get_dir_size(path):
    total = 0
    for root, _, files in os.walk(path):
        for f in files:
            fp = os.path.join(root, f)
            if os.path.isfile(fp):
                total += os.path.getsize(fp)
    return total

def human_readable(size_bytes):
    for unit in ["B", "KB", "MB", "GB", "TB"]:
        if size_bytes < 1024:
            return f"{size_bytes:.2f} {unit}"
        size_bytes /= 1024
    return "PB"

# ---------- UPLOAD ----------
@app.route("/files/upload", methods=["POST"])
def upload_file():
    user_id = request.form.get("userId")
    file = request.files.get("file")

    if not user_id or not file:
        return jsonify({"error": "userId and file required"}), 400

    file_id = str(uuid.uuid4())
    user_dir = os.path.join(BASE_STORAGE, user_id)
    os.makedirs(user_dir, exist_ok=True)

    safe_name = file.filename.replace("/", "_")
    file_path = os.path.join(user_dir, f"{file_id}_{safe_name}")

    file.save(file_path)

    return jsonify({
        "userId": user_id,
        "fileId": file_id,
        "url": f"/{user_id}/{file_id}"
    }), 201

@app.route("/<user_id>/<file_id>", methods=["GET"])
def get_file(user_id, file_id):
    user_dir = os.path.join(BASE_STORAGE, user_id)

    if not os.path.isdir(user_dir):
        abort(404)

    matches = glob.glob(os.path.join(user_dir, f"{file_id}_*"))

    if not matches:
        abort(404)

    file_path = matches[0]

    return send_file(
        file_path,
        as_attachment=True
    )
@app.route("/files", methods=["GET"])
def list_all_files():
    all_files = []

    for user_id in os.listdir(BASE_STORAGE):
        user_dir = os.path.join(BASE_STORAGE, user_id)
        if not os.path.isdir(user_dir):
            continue

        for fname in os.listdir(user_dir):
            if "_" not in fname:
                continue

            file_id, original_name = fname.split("_", 1)
            all_files.append({
                "userId": user_id,
                "fileId": file_id,
                "fileName": original_name,
                "url": f"/{user_id}/{file_id}"
            })

    return jsonify({
        "count": len(all_files),
        "files": all_files
    })
@app.route("/storage/usage", methods=["GET"])
def total_storage_usage():
    size = get_dir_size(BASE_STORAGE)

    return {
        "bytes": size,
        "usage": human_readable(size)
    }
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)

