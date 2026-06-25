from fastapi import FastAPI

app = FastAPI(title="养老 AI 伴诊服务")

@app.get("/health")
def health():
    return {"status": "ok"}