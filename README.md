# Notifications Lab

### Notifications A15
L'applicazione è stata sviluppata per mettere in evidenza i vari tipi di notifica disponibili
nell'ecosistema Android.
In particolare, si è scelto di rappresentare notifiche:
 - semplici 
 - espandibili (con testo o immagine)
 - con azioni rapide o che prendono un input di testo dall'utente
 - con barra di progresso (stato di avanzamento)
 - di chiamata in arrivo ed in corso
 - con media player integrato

Particolare enfasi è stata posta sulle notifiche semplici ed espandibili, offrendo all'utente
la possibilità di collegarsi a [OpenWeatherMap](https://openweathermap.org/) e a
[GNews](https://gnews.io/) per ottenere periodicamente aggiornamenti meteo (semplici) e notizie di
cronaca (espandibili) rispettivamente.<br/>
Si consiglia di disattivare eventuali ottimizzazioni per la batteria nelle impostazioni di sistema,
in quanto potrebbero impedire la pubblicazione di queste ultime quando l'applicazione è chiusa per
un periodo di tempo prolungato.

È stata inoltre implementata una semplice chat con un assistente virtuale, offerta da
[Together](https://www.together.ai/) usando il modello generativo
'meta-llama/Llama-3.3-70B-Instruct-Turbo-Free'