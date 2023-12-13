/** @type {import('tailwindcss').Config} */
export default {
	content: ['./src/**/*.{html,js,svelte,ts}'],
	safelist: ['main', 'bg-red-500', 'bg-blue-500', 'bg-yellow-500', 'bg-cyan-500'],
	theme: {
		extend: {}
	},
	plugins: []
};
