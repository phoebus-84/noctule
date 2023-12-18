<script lang="ts" context="module">
	import type { Filter } from '$lib/ffmpeg/FFmpeg';

	export interface StepperContext {
		video: { name: string | undefined; path: string | undefined; video: any } | undefined;
		filter: Filter<{ [s: string]: unknown } | null> | undefined;
		results: { value: string; input: string } | undefined;
	}
</script>

<script lang="ts">
	import ApplyFilter from '$lib/newFilterSteps/ApplyFilter.svelte';

	import PickVideo from '$lib/newFilterSteps/PickVideo.svelte';
	import Results from '$lib/newFilterSteps/Results.svelte';

	import { Stepper, makeStep } from '@efstajas/svelte-stepper';
	import { writable, type Writable } from 'svelte/store';

	const steps = [
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
			filter: undefined,
			results: undefined
		});
</script>

<ion-content fullscreen class="ion-padding">
	<Stepper defaultTransitionDuration={0} {steps} {context} />
</ion-content>
