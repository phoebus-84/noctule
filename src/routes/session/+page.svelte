<script lang="ts">
	import { page } from '$app/stores';
	import FFmpeg from '$lib/nativeHooks/FFmpegPlugin';

	const url = $page.url;
	const id = url.searchParams.get('id');
	const session = async () => await FFmpeg.getSession({ sessionId: Number(id) });
</script>

<ion-content>
	{#await session()}
		<ion-spinner />
	{:then session}
		<pre>
            {JSON.stringify(session, null, 2)}
        </pre>
	{/await}
	<pre></pre>
	<ion-button href="/">main</ion-button>
</ion-content>
