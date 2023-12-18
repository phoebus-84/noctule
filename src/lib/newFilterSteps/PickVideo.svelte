<script lang="ts">
	import type { Writable } from 'svelte/store';
	import type { StepperContext } from '../../routes/new/+page.svelte';
	import { createStepController } from '@efstajas/svelte-stepper';
	import { FilePicker } from '@capawesome/capacitor-file-picker';

	const stepController = createStepController();

	export let context: Writable<StepperContext>;

	FilePicker.pickFiles({
		readData: false,
		multiple: false
	})
		.then((r) => {
			const path = r.files[0].path;
			const name = r.files[0].name;
			if (path && name) $context.video = { path, name, video: r.files[0] };
			stepController.nextStep();
		})
		.catch((e) => {
			console.error(e);
		});
</script>
