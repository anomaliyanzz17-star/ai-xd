
import streamlit as st
from groq import Groq

st.set_page_config(
    page_title="AI-XD",
    page_icon="🤖"
)

st.title("🤖 AI-XD")
st.caption("Chatbot AI buatan Nael")

if "messages" not in st.session_state:
    st.session_state.messages = []

# Tampilkan riwayat percakapan
for chat in st.session_state.messages:
    with st.chat_message(chat["role"]):
        st.write(chat["content"])

# Input pesan
pesan = st.chat_input("Ketik pesan ke AI-XD...")

if pesan:
    st.session_state.messages.append({
        "role": "user",
        "content": pesan
    })

    with st.chat_message("user"):
        st.write(pesan)

    try:
        client = Groq(
            api_key=st.secrets["GROQ_API_KEY"]
        )

        riwayat = [
            {
                "role": "system",
                "content": (
                    "Nama kamu AI-XD, asisten virtual "
                    "buatan Nael. Jawab dalam bahasa "
                    "Indonesia yang santai, jelas, dan "
                    "mudah dipahami. Bantu pengguna "
                    "dengan pertanyaan, pelajaran, dan coding."
                )
            }
        ] + st.session_state.messages[-12:]

        with st.chat_message("assistant"):
            with st.spinner("AI-XD sedang berpikir..."):
                response = client.chat.completions.create(
                    model="llama-3.3-70b-versatile",
                    messages=riwayat
                )

                balasan = response.choices[0].message.content

            st.write(balasan)

        st.session_state.messages.append({
            "role": "assistant",
            "content": balasan
        })

    except Exception as e:
        st.error(
            "AI-XD mengalami masalah. "
            "Periksa API Key, Secrets, atau kuota Groq."
        )
        
