<script lang="ts" context="module">
	import type { Filter } from '$lib/ffmpeg/FFmpeg';

	export interface StepperContext {
		video: { name: string | undefined; path: string | undefined; video: any } | undefined;
		filter: Filter<{ [s: string]: unknown } | null> | undefined;
	}
</script>

<script lang="ts">
	import ApplyFilter from '$lib/newFilterSteps/ApplyFilter.svelte';

	import PickVideo from '$lib/newFilterSteps/PickVideo.svelte';
	import Results from '$lib/newFilterSteps/Results.svelte';

	import { Stepper, makeStep } from '@efstajas/svelte-stepper';
	import { writable, type Writable } from 'svelte/store';

	const exampleSteps = [
		makeStep({
			component: PickVideo,
			props: undefined
		}),
		makeStep({
			component: ApplyFilter,
			props: undefined
		}),
		makeStep({
			component: Results,
			props: undefined
		})
	];

	const context: () => Writable<StepperContext> = () =>
		writable({
			video: undefined,
			filter: undefined
		});
</script>

<Stepper steps={exampleSteps} {context} />
