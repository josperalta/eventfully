document.addEventListener('DOMContentLoaded', () => {
  const subscribeButton = document.getElementById('subscribe-button');
  if (subscribeButton) {
    subscribeButton.onclick = function() {
      let currentPath = window.location.pathname;
      if (currentPath.endsWith('/')) {
        currentPath = currentPath.slice(0, -1);
      } else if (currentPath.endsWith('.html')) {
        currentPath = currentPath.slice(0, -5);
      }

      location.href = `/content/eventfully/us/en/register.html?event=${currentPath}`
    }
  }
})