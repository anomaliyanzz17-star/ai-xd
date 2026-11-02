import streamlit as st
from groq import Groq

# =========================
# KONFIGURASI
# =========================

st.set_page_config(
    page_title="AI-XD",
    page_icon="🤖",
    layout="centered"
)

st.title("🤖 AI-XD")
st.caption("Asisten AI buatan Nael")

# =========================
# CEK API KEY
# =========================

try:
    api_key = st.secrets["GROQ_API_KEY"]
    client = Groq(api_key=api_key)
except Exception:
    st.error(
        "API Key Groq belum terbaca. "
        "Periksa Streamlit Secrets."
    )
    st.stop()

# =========================
# RIWAYAT CHAT
# =========================

if "messages" not in st.session_state:
    st.session_state.messages = []

# Tombol hapus percakapan
with st.sidebar:
    st.subheader("Pengaturan AI-XD")

    if st.button("🗑️ Hapus percakapan"):
        st.session_state.messages = []
        st.rerun()

    st.caption("AI-XD versi 2.0")

# Tampilkan chat sebelumnya
for chat in st.session_state.messages:
    with st.chat_message(chat["role"]):
        st.markdown(chat["content"])

# =========================
# INPUT PESAN
# =========================

pesan = st.chat_input("Tanyakan sesuatu kepada AI-XD...")

if pesan:

    # Simpan pesan pengguna
    st.session_state.messages.append({
        "role": "user",
        "content": pesan
    })

    with st.chat_message("user"):
        st.markdown(pesan)

    # Instruksi ke AI
    system_message = {
        "role": "system",
        "content": """
        Nama kamu AI-XD.
        Kamu adalah asisten AI buatan Nael.

        Gunakan bahasa Indonesia yang santai,
        natural, jelas, dan mudah dipahami.

        Kamu bisa membantu:
        - Menjawab pertanyaan umum.
        - Menjelaskan pelajaran.
        - Membantu pemrograman.
        - Memberikan ide kreatif.
        - Melakukan percakapan sehari-hari.

        Jawablah sesuai konteks percakapan.
        Jangan mengarang fakta jika tidak tahu.
        """
    }

    # Ambil riwayat percakapan
    riwayat = [
        system_message
    ] + st.session_state.messages[-20:]

    # =========================
    # HUBUNGKAN KE GROQ
    # =========================

    with st.chat_message("assistant"):

        try:
            with st.spinner("AI-XD sedang berpikir..."):

                response = client.chat.completions.create(
                    model="llama-3.3-70b-versatile",
                    messages=riwayat,
                    temperature=0.7,
                    max_completion_tokens=1024
                )

                balasan = response.choices[0].message.content

            st.markdown(balasan)

            # Simpan jawaban AI
            st.session_state.messages.append({
                "role": "assistant",
                "content": balasan
            })

        except Exception as e:
            st.error("AI-XD gagal menjawab.")
            st.code(f"{type(e).__name__}: {e}")
            
