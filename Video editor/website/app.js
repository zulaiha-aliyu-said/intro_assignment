(function () {
  const versionEl = document.getElementById("version-name");
  const codeEl = document.getElementById("version-code");
  const dateEl = document.getElementById("released-at");
  const androidEl = document.getElementById("min-android");
  const notesEl = document.getElementById("release-notes");
  const downloadBtn = document.getElementById("download-btn");
  const statusEl = document.getElementById("download-status");
  const appIdEl = document.getElementById("application-id");

  function setStatus(message, isError) {
    statusEl.textContent = message || "";
    statusEl.classList.toggle("error", Boolean(isError));
  }

  function renderManifest(data) {
    versionEl.textContent = data.versionName || "—";
    codeEl.textContent = data.versionCode != null ? String(data.versionCode) : "—";
    dateEl.textContent = data.releasedAt || "—";
    androidEl.textContent = data.minAndroid || "Android 8.0+";
    if (appIdEl) appIdEl.textContent = data.applicationId || "com.capcutmini.app";

    notesEl.innerHTML = "";
    const notes = Array.isArray(data.releaseNotes) ? data.releaseNotes : [];
    if (notes.length === 0) {
      const li = document.createElement("li");
      li.textContent = "No release notes listed yet.";
      notesEl.appendChild(li);
    } else {
      notes.forEach(function (note) {
        const li = document.createElement("li");
        li.textContent = note;
        notesEl.appendChild(li);
      });
    }

    const url = (data.downloadUrl || "").trim();
    if (url) {
      downloadBtn.href = url;
      downloadBtn.removeAttribute("aria-disabled");
      downloadBtn.setAttribute("download", data.apkFileName || "CapCutMini.apk");
      setStatus("Ready to download " + (data.apkFileName || "the latest APK") + ".");
    } else {
      downloadBtn.href = "#";
      downloadBtn.setAttribute("aria-disabled", "true");
      setStatus(
        "APK link not published yet. After you upload a GitHub Release, set downloadUrl in version.json.",
        true
      );
    }
  }

  fetch("version.json", { cache: "no-store" })
    .then(function (res) {
      if (!res.ok) throw new Error("Could not load version.json (" + res.status + ")");
      return res.json();
    })
    .then(renderManifest)
    .catch(function (err) {
      setStatus(err.message || "Failed to load version info.", true);
      downloadBtn.setAttribute("aria-disabled", "true");
    });

  downloadBtn.addEventListener("click", function (event) {
    if (downloadBtn.getAttribute("aria-disabled") === "true") {
      event.preventDefault();
    }
  });
})();
