<script lang="ts">
	import type { Writable } from 'svelte/store';
	import type { StepperContext } from '../../routes/new/+page.svelte';
	import Logs from '$lib/logs/Logs.svelte';
	import { goto } from '$app/navigation';

	export let context: Writable<StepperContext>;
</script>

<ion-list>
	<ion-item>
		<ion-label class="font-bold text-blue-400">Command:</ion-label>
	</ion-item>
	<ion-item>
		<code
			class="my-2 inline-flex items-center space-x-4 rounded-lg bg-gray-800 p-4 pl-6 text-left text-sm text-white sm:text-base"
		>
			<span class="flex gap-4">
				<span class="shrink-0 text-gray-500"> $ </span>
				<span class="flex-1">
					<span class="text-yellow-500">ffmpeg -i [INPUT] {$context.filter?.getCommand()} [OUTPUT]</span>
				</span>
			</span>
		</code>
	</ion-item>
     <ion-item>
            {$context.results?.statistics}
    </ion-item>
	<ion-item>
		<ion-label class="font-bold text-blue-400">Logs:</ion-label>
	</ion-item>
   
	<ion-item>
		<Logs logString={$context.results?.logs} />
	</ion-item>
	<ion-item>
		<ion-button slot="end" on:click={() => goto('/')}>exit</ion-button>
	</ion-item>
</ion-list>
