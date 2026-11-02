import streamlit as st

st.set_page_config(
    page_title="AI-XD",
    page_icon="🤖"
)

st.title("🤖 AI-XD")
st.caption("Chatbot buatan Nael")

def jawab(pesan):
    teks = pesan.lower()

    if "halo" in teks or "hai" in teks:
        return "Halo! Selamat datang di AI-XD."

    elif "nama" in teks:
        return "Nama gw AI-XD, chatbot buatan Nael!"

    elif "apa kabar" in teks:
        return "Gw baik! Lu sendiri gimana?"

    elif "siapa kamu" in teks:
        return "Gw AI-XD, asisten virtual sederhana."

    elif "terima kasih" in teks:
        return "Sama-sama! Senang bisa membantu."

    elif "bantuan" in teks:
        return "Tulis pertanyaan atau pesan yang ingin lu tanyakan."

    else:
        return "Menarik! Tapi gw belum paham pertanyaan itu."

if "messages" not in st.session_state:
    st.session_state.messages = []

for chat in st.session_state.messages:
    with st.chat_message(chat["role"]):
        st.write(chat["content"])

pesan = st.chat_input("Ketik pesan ke AI-XD...")

if pesan:
    st.session_state.messages.append({
        "role": "user",
        "content": pesan
    })

    with st.chat_message("user"):
        st.write(pesan)

    balasan = jawab(pesan)

    st.session_state.messages.append({
        "role": "assistant",
        "content": balasan
    })

    with st.chat_message("assistant"):
        st.write(balasan)
