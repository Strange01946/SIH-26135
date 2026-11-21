import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        navy: {
          50: '#e8eef7',
          100: '#c5d3ea',
          200: '#9fb6db',
          300: '#7a99cc',
          400: '#5f83c0',
          500: '#4470b4',
          600: '#3a63a5',
          700: '#2e5293',
          800: '#234181',
          900: '#1a3c6e',
          DEFAULT: '#1a3c6e',
        },
        saffron: '#FF9933',
        'india-green': '#138808',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}

export default config
