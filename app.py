
import streamlit as st
import json
import urllib.request
import urllib.error

st.set_page_config(
    page_title="AI-XD",
    page_icon="🤖",
    layout="centered"
)

BACKEND_URL = "https://ai-xd-backend.anomaliyanzz17.workers.dev/chat"

st.title("🤖 AI-XD")
st.caption("Asisten AI buatan Nael")

# Riwayat percakapan
if "messages" not in st.session_state:
    st.session_state.messages = []

# Sidebar
with st.sidebar:
    st.subheader("Pengaturan AI-XD")

    if st.button("🗑️ Hapus percakapan"):
        st.session_state.messages = []
        st.rerun()

    st.caption("AI-XD versi 2.2")

# Fungsi menghubungkan backend
def kirim_pesan(messages):
    payload = json.dumps({
        "messages": messages
    }).encode("utf-8")

    request = urllib.request.Request(
        BACKEND_URL,
        data=payload,
        headers={
            "Content-Type": "application/json"
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(request, timeout=90) as response:
            data = json.loads(response.read().decode("utf-8"))

        if not data.get("success"):
            raise Exception(data.get("error", "Backend gagal menjawab."))

        return data["reply"]

    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")
        raise Exception(f"HTTP {e.code}: {detail}")

# Menampilkan riwayat chat
for chat in st.session_state.messages:
    with st.chat_message(chat["role"]):
        st.markdown(chat["content"])

# Input pesan
pesan = st.chat_input("Tanyakan sesuatu kepada AI-XD...")

if pesan:
    st.session_state.messages.append({
        "role": "user",
        "content": pesan
    })

    with st.chat_message("user"):
        st.markdown(pesan)

    with st.chat_message("assistant"):
        try:
            with st.spinner("AI-XD sedang berpikir..."):
                riwayat = st.session_state.messages[-20:]
                balasan = kirim_pesan(riwayat)

            st.markdown(balasan)

            st.session_state.messages.append({
                "role": "assistant",
                "content": balasan
            })

        except Exception as e:
            st.error("AI-XD gagal menjawab.")
            st.code(str(e))
        
