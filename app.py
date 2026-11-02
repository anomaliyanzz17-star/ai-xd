
import streamlit as st
import json
import urllib.request
import urllib.error

# =========================
# KONFIGURASI AI-XD
# =========================

st.set_page_config(
    page_title="AI-XD",
    page_icon="🤖",
    layout="centered"
)

BACKEND_URL = "https://ai-xd-backend.anomaliyanzz17.workers.dev/chat"

# =========================
# IDENTITAS APLIKASI
# =========================

st.title("🤖 AI-XD")
st.caption("Asisten AI buatan Nael • Versi 2.3")

# =========================
# SESSION STATE
# =========================

if "messages" not in st.session_state:
    st.session_state.messages = []

# =========================
# SIDEBAR
# =========================

with st.sidebar:
    st.subheader("Pengaturan AI-XD")

    st.caption("Dikembangkan oleh Nael")

    if st.button("🗑️ Hapus percakapan", use_container_width=True):
        st.session_state.messages = []
        st.rerun()

    st.divider()
    st.caption("AI-XD versi 2.3")
    st.caption("Powered by Groq")

# =========================
# FUNGSI BACKEND
# =========================

def kirim_pesan(messages):

    payload = json.dumps({
        "messages": messages
    }).encode("utf-8")

    request = urllib.request.Request(
        BACKEND_URL,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "User-Agent": "AI-XD/2.3"
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(
            request,
            timeout=90
        ) as response:

            data = json.loads(
                response.read().decode("utf-8")
            )

        if not data.get("success"):
            raise Exception(
                data.get("error", "Backend gagal menjawab.")
            )

        return data["reply"]

    except urllib.error.HTTPError as e:

        detail = e.read().decode(
            "utf-8",
            errors="replace"
        )

        raise Exception(
            f"HTTP {e.code}: {detail}"
        )

    except urllib.error.URLError as e:

        raise Exception(
            f"Koneksi backend gagal: {e.reason}"
        )

# =========================
# TAMPILKAN RIWAYAT CHAT
# =========================

for chat in st.session_state.messages:

    with st.chat_message(chat["role"]):
        st.markdown(chat["content"])

# =========================
# INPUT PENGGUNA
# =========================

pesan = st.chat_input(
    "Tanyakan sesuatu kepada AI-XD..."
)

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

            st.info(
                "Periksa koneksi backend atau konfigurasi layanan AI."
            )
            
