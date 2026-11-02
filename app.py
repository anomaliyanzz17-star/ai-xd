
import streamlit as st
from groq import Groq

st.set_page_config(
    page_title="AI-XD",
    page_icon="🤖",
    layout="centered"
)

st.title("🤖 AI-XD")
st.caption("Asisten AI buatan Nael")

# Menghubungkan API Groq
try:
    api_key = st.secrets["GROQ_API_KEY"]
    client = Groq(api_key=api_key)
except Exception:
    st.error("API Key Groq belum terbaca. Periksa Streamlit Secrets.")
    st.stop()

# Menyimpan riwayat percakapan
if "messages" not in st.session_state:
    st.session_state.messages = []

# Sidebar
with st.sidebar:
    st.subheader("Pengaturan AI-XD")

    if st.button("🗑️ Hapus percakapan"):
        st.session_state.messages = []
        st.rerun()

    st.caption("AI-XD versi 2.1")

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

    riwayat = [
        system_message
    ] + st.session_state.messages[-20:]

    with st.chat_message("assistant"):
        try:
            with st.spinner("AI-XD sedang berpikir..."):
                response = client.chat.completions.create(
                    model="openai/gpt-oss-120b",
                    messages=riwayat,
                    temperature=0.7,
                    max_completion_tokens=1024
                )

                balasan = response.choices[0].message.content

            st.markdown(balasan)

            st.session_state.messages.append({
                "role": "assistant",
                "content": balasan
            })

        except Exception as e:
            st.error("AI-XD gagal menjawab.")
            st.code(f"{type(e).__name__}: {e}")
            
