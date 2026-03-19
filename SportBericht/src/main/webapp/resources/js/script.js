
function handleEmailDialog(emailBereitsGesendet) {
    console.log("emailBereitsGesendet =", emailBereitsGesendet); // optional zum Testen
    if (emailBereitsGesendet === true) {
        PF('dialogBereitsGesendet').show();
    } else {
        PF('dialogNochNichtGesendet').show();
    }
}

function updatePreview(previewClass, value) {
    const preview = document.querySelector('.' + previewClass);
    if (preview) {
        preview.style.backgroundColor = 'hsl(' + value + ', 90%, 50%)';
    }
}

let isDragging = false;
let startX, startY;

function startSelection(event) {
	const rect = event.target.getBoundingClientRect();
	startX = event.clientX - rect.left;
	startY = event.clientY - rect.top;
	isDragging = true;

	const selectionBox = document.getElementById("selectionRectangle");
	selectionBox.style.left = startX + "px";
	selectionBox.style.top = startY + "px";
	selectionBox.style.width = "0px";
	selectionBox.style.height = "0px";
	selectionBox.style.display = "block";
}

function duringSelection(event) {
	if (!isDragging)
		return;

	const rect = event.target.getBoundingClientRect();
	const currentX = event.clientX - rect.left;
	const currentY = event.clientY - rect.top;

	const selectionBox = document.getElementById("selectionRectangle");

	const x = Math.min(currentX, startX);
	const y = Math.min(currentY, startY);
	const width = Math.abs(currentX - startX);
	const height = Math.abs(currentY - startY);

	selectionBox.style.left = x + "px";
	selectionBox.style.top = y + "px";
	selectionBox.style.width = width + "px";
	selectionBox.style.height = height + "px";
}

function endSelection(event) {
	if (!isDragging)
		return;
	isDragging = false;

	const rect = event.target.getBoundingClientRect();
	const endX = event.clientX - rect.left;
	const endY = event.clientY - rect.top;

	const cropX = Math.min(startX, endX);
	const cropY = Math.min(startY, endY);
	const cropWidth = Math.abs(endX - startX);
	const cropHeight = Math.abs(endY - startY);

	// Werte in Hidden Input (für Managed Bean)
	document.getElementById("myForm:cropXHidden").value = Math
			.round(cropX);
	document.getElementById("myForm:cropYHidden").value = Math
			.round(cropY);
	document.getElementById("myForm:cropWidthHidden").value = Math
			.round(cropWidth);
	document.getElementById("myForm:cropHeightHidden").value = Math
			.round(cropHeight);

	// Werte aktualisieren, sichtbar für User
	document.getElementById("myForm:cropXOutput").innerHTML = Math
			.round(cropX);
	document.getElementById("myForm:cropYOutput").innerHTML = Math
			.round(cropY);
	document.getElementById("myForm:cropWidthOutput").innerHTML = Math
			.round(cropWidth);
	document.getElementById("myForm:cropHeightOutput").innerHTML = Math
			.round(cropHeight);

	// sichtbar lassen, nicht verstecken!
	const selectionBox = document.getElementById("selectionRectangle");
	selectionBox.style.left = cropX + "px";
	selectionBox.style.top = cropY + "px";
	selectionBox.style.width = cropWidth + "px";
	selectionBox.style.height = cropHeight + "px";
	selectionBox.style.display = "block"; // bleibt dauerhaft sichtbar!

	// JSF Remote Command aufrufen (Server-Seite aktualisiert)
	sendCoordinatesToServer();
}


function resizeImage(input) {
	if (input.files && input.files[0]) {
		var file = input.files[0];
		var reader = new FileReader();
		reader.onload = function(e) {
			var img = new Image();
			img.src = e.target.result;
			img.onload = function() {
				var maxSize = 1500; // Maximale Breite/Höhe in Pixeln
				var width = img.width;
				var height = img.height;

				// Berechne, ob Skalierung nötig ist
				if (width > maxSize || height > maxSize) {
					var scale = Math.min(maxSize / width, maxSize / height);
					width = Math.round(width * scale);
					height = Math.round(height * scale);
				}

				// Erstelle ein Canvas mit den neuen Dimensionen
				var canvas = document.createElement("canvas");
				canvas.width = width;
				canvas.height = height;
				var ctx = canvas.getContext("2d");
				ctx.drawImage(img, 0, 0, width, height);

				// Konvertiere das Canvas in einen Blob im JPEG-Format mit 75% Qualität
				canvas.toBlob(function(blob) {
					// Erstelle ein neues File-Objekt aus dem Blob
					var resizedFile = new File([blob], file.name, {
						type: "image/jpeg",
						lastModified: Date.now()
					});

					// Ersetze die ursprüngliche Datei im File-Input durch die verkleinerte Datei
					var dataTransfer = new DataTransfer();
					dataTransfer.items.add(resizedFile);
					input.files = dataTransfer.files;

					// Zusätzlich: Erzeuge einen Base64-String des verkleinerten Bildes
					var reader2 = new FileReader();
					reader2.onload = function(evt) {
						// Schreibe den Base64-String in das versteckte Input-Feld
						var hiddenField = document.getElementById("hiddenImageBase64");
						if (hiddenField) {
							hiddenField.value = evt.target.result;
						}
					};
					reader2.readAsDataURL(resizedFile);
				}, "image/jpeg", 0.8);
			};
		};
		reader.readAsDataURL(file);
	}
}

function sendHeight() {
	var height = document.documentElement.scrollHeight;
	window.parent.postMessage(height, '*');
}

window.addEventListener('load', sendHeight);
window.addEventListener('resize', sendHeight);

var observer = new MutationObserver(function() {
	sendHeight();
});

observer.observe(document.body, {
	childList: true,
	subtree: true
});

// Bei PrimeFaces AJAX-Aktualisierungen erneut Höhe senden
if (window.PrimeFaces) {
	PrimeFaces.ajax.Queue.add({
		oncomplete: function() {
			sendHeight();
		}
	});
}

document.addEventListener('DOMContentLoaded', function() {
	if (window.PF && PF('logPanelWidget')) {
		PF('logPanelWidget').jq.on('toggle', function() {
			setTimeout(sendHeight, 300); // Warten bis Animation fertig
		});
	}
});

// Update der Berichtgröße bei Slidefeld
document.getElementById('reportSize').addEventListener('input', function() {
	document.getElementById('reportSizeValue').textContent = this.value;
});

// Funktion zum Generieren des Berichts
/*function generateReport() {
	var reportSize = document.getElementById('reportSize').value;
	var specialIncidents = document.getElementById('specialIncidents').value;

	var report = "Bericht (Größe: " + reportSize + "):\n\n";
	if (specialIncidents.trim() !== "") {
		report += "Besondere Vorkommnisse:\n" + specialIncidents + "\n\n";
	}
	report += "Der Bericht wird nun generiert...";

	document.getElementById('generatedReport').textContent = report;

	// Hier könnte eine Java-Funktion aufgerufen werden (dies wäre eine Integration auf der Serverseite)
	// z.B. durch ein Ajax-Request zu einer Java-Servlet-Methode
}*/

// Funktion, um den Bericht zu speichern (z.B. in einer Datenbank)
/*function saveReport() {
	var reportSize = document.getElementById('reportSize').value;
	var specialIncidents = document.getElementById('specialIncidents').value;
	var report = "Bericht (Größe: " + reportSize + "):\n\n";
	if (specialIncidents.trim() !== "") {
		report += "Besondere Vorkommnisse:\n" + specialIncidents + "\n\n";
	}

	// Hier müsste eine Anfrage an den Server gesendet werden, um den Bericht in der Datenbank zu speichern.
	// Beispiel einer Ajax-Anfrage (dies funktioniert nur in einer richtigen Server-Umgebung):
	
	var xhr = new XMLHttpRequest();
	xhr.open("POST", "saveReport", true); // 'saveReport' ist ein Beispiel für eine Server-URL
	xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status == 200) {
			alert("Bericht gespeichert!");
		}
	};
	xhr.send("report=" + encodeURIComponent(report));
	

	// Zum Testen: Ausgabe in das Textfeld
	document.getElementById('reportOutput').value = report;
	alert("Bericht wurde übernommen und gespeichert.");
}*/

/*// Funktion zum Zurücknavigieren zur 'bericht.xhtml'
function goBack() {
	window.location.href = 'bericht.xhtml';
}*/

function kopiereHtmlBerichtZusammen() {
	const hiddenField = document.getElementById('meinFormular:berichtHidden');
	const htmlContent = hiddenField ? hiddenField.value : "test";

	if (htmlContent && navigator.clipboard && window.ClipboardItem) {
		// Optional: Plaintext extrahieren, falls Ziel das benötigt
		const tempDiv = document.createElement("div");
		tempDiv.innerHTML = htmlContent;
		const textContent = tempDiv.innerText;

		const clipboardItem = new ClipboardItem({
			"text/html": new Blob([htmlContent], { type: "text/html" }),
			"text/plain": new Blob([textContent], { type: "text/plain" })
		});

		navigator.clipboard.write([clipboardItem]).then(() => {
			alert("Bericht mit Formatierungen wurde in die Zwischenablage kopiert.");
		}).catch(err => {
			console.error("Fehler beim Kopieren:", err);
			alert("Kopieren fehlgeschlagen.");
		});
	} else {
		alert("Clipboard API wird nicht unterstützt oder Inhalt fehlt. " + htmlContent + "Hallo");
	}
}

function kopiereHtmlBericht() {
	const hiddenField = document.getElementById('mainForm:berichtHidden');
	const htmlContent = hiddenField ? hiddenField.value : "test";

	if (htmlContent && navigator.clipboard && window.ClipboardItem) {
		// Optional: Plaintext extrahieren, falls Ziel das benötigt
		const tempDiv = document.createElement("div");
		tempDiv.innerHTML = htmlContent;
		const textContent = tempDiv.innerText;

		const clipboardItem = new ClipboardItem({
			"text/html": new Blob([htmlContent], { type: "text/html" }),
			"text/plain": new Blob([textContent], { type: "text/plain" })
		});

		navigator.clipboard.write([clipboardItem]).then(() => {
			alert("Bericht mit Formatierungen wurde in die Zwischenablage kopiert.");
		}).catch(err => {
			console.error("Fehler beim Kopieren:", err);
			alert("Kopieren fehlgeschlagen.");
		});
	} else {
		alert("Clipboard API wird nicht unterstützt oder Inhalt fehlt. " + htmlContent + "Hallo");
	}
}

