/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        amber: {
          glow: '#fbbf24',
          warm: '#f59e0b',
        }
      }
    },
  },
  plugins: [],
}
