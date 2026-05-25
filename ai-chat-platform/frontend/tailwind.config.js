/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#172026',
        surface: '#f7f5ef',
        accent: '#0f766e',
      },
      boxShadow: {
        panel: '0 18px 50px rgba(23, 32, 38, 0.10)',
      },
    },
  },
  plugins: [],
};
