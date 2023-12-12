<script lang="ts">
	type Log = {
		sessionId: string;
		level: string;
		message: string;
	};

	export let logString: string | undefined;

	let logs: (Log | null)[] = [];

	let logArray: any[] = [];
	function parseLogs() {
		try {
			const regex = /Log\{sessionId=(.*?),\s*level=(.*?),\s*message='(.*?[^\\])'\}/g;
			let matches;

			while ((matches = regex.exec(logString || '')) !== null) {
				logs.push({
					sessionId: matches[1],
					level: matches[2],
					message: matches[3]
				});
			}
		} catch (error) {
			console.error('Error parsing log string:', error);
		}
	}
	const colorLevel = (level: string) => {
		switch (level) {
			case 'AV_LOG_FATAL':
				return 'red-500';
			case 'AV_LOG_ERROR':
				return 'red-500';
			case 'AV_LOG_WARNING':
				return 'yellow-500';
			case 'AV_LOG_INFO':
				return 'blue-500';
			case 'AV_LOG_DEBUG':
				return 'cyan-500';
		}
	};

	$: parseLogs();
</script>

<div class="w-full max-h-96 overflow-y-auto bg-gray-800 p-4 text-gray-200">
	{#each logs as log}
		{#if log}
			<div class="border-b-1 mt-0.5 border-gray-600">
                <div class="hidden bg-blue-500 bg-red-500"/>
				<ion-chip class={`bg-${colorLevel(log.level)} text-white text-xs rounded-sm`}>
					{log.level}
				</ion-chip>
				<!-- <span class={`text-bold px-2 py-1`}></span> -->
				<!-- <span class="font-weight-light text-gray-400">{log.sessionId}</span> -->
				{log.message}
			</div>
		{/if}
	{/each}
</div>
