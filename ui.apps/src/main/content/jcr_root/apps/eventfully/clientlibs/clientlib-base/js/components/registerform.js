document.addEventListener('DOMContentLoaded', () => {
  const eventInput = document.getElementById('subscribe-button');
  if (eventInput) {
    let eventParam = window.location.search.split('event=')[1];
    if (eventParam) {
      eventParam = eventParam.split('&')[0];
      document.getElementById('event').value = eventParam;
    }
  }
})