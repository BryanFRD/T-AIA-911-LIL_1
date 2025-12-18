import type { Config } from 'tailwindcss';

const config: Config = {
  content: ["./App.{ts,tsx}", "./**/*.{ts,tsx}"],
  theme: {
    extend: {},
  },
  plugins: [],
};

export default config;
