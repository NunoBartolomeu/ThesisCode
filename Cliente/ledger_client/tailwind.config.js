module.exports = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx}",     // for Next.js app router files
    "./pages/**/*.{js,ts,jsx,tsx}",   // for old pages router files, if any
    "./components/**/*.{js,ts,jsx,tsx}",
    "./src/**/*.{js,ts,jsx,tsx}"      // if your code is under src/
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          light: '#e0e7ff',
          DEFAULT: '#6366f1',  // purple-500
          dark: '#4338ca'
        },
        background: {
          light: '#ffffff',
          dark: '#0f172a'
        },
        text: {
          light: '#1e293b',
          dark: '#f8fafc'
        }
      }
    }
  },
  plugins: []
}
