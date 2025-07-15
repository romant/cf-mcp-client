const { createApp, ref } = Vue;

createApp({
  setup() {
    const messages = ref([]);
    const current = ref('');

    function send() {
      if (!current.value) return;
      const text = current.value;
      messages.value.push({ role: 'user', content: text });
      current.value = '';

      const source = new EventSource(`/chat?chat=${encodeURIComponent(text)}`);
      source.addEventListener('message', (e) => {
        const data = JSON.parse(e.data);
        messages.value.push({ role: 'assistant', content: data.content });
      });
      source.addEventListener('close', () => {
        source.close();
      });
    }

    return { messages, current, send };
  }
}).mount('#app');
