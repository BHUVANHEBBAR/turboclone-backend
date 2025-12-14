from pymongo import MongoClient
from bson import ObjectId
from dotenv import load_dotenv
from pydantic import BaseModel
from typing import List
from fastapi import FastAPI, HTTPException
import os

load_dotenv()
class OCRRequest(BaseModel):
    jobId: str
    resourceLinks: List[str]
client = MongoClient(os.getenv("MONGODB_URI"))
db = client.turbotestdb
job_col = db.job

job_id = "69381617b2bfb07a67b6f849"

doc = job_col.find_one({
    "_id": ObjectId(job_id)
})

print(doc)
app=FastAPI()
@app.post("/ocr")
def start_ocr(req: OCRRequest):
    if not req.jobId:
        raise HTTPException(status_code=400, detail="jobId is required")

    if not req.resourceLinks or len(req.resourceLinks) == 0:
        raise HTTPException(status_code=400, detail="resourceLinks cannot be empty")

    return {
        "jobId": req.jobId,
        "status": "ACCEPTED",
        "resourceCount": len(req.resourceLinks)
    }
