const STORAGE_KEY = "ict-web-register-state-v1";
const remoteBackend = window.assetBackend;
const remoteMode = Boolean(remoteBackend?.enabled);

const seedState = {
  currentUser: null,
  users: [
    { id: "u1", fullName: "Municipal ICT Admin", username: "admin", password: "admin123", role: "Admin", active: true },
    { id: "u2", fullName: "Standard Field User", username: "standard", password: "user123", role: "Standard User", active: true },
    { id: "u3", fullName: "Thabo Mokoena", username: "tech", password: "tech123", role: "ICT Technician", active: true },
    { id: "u4", fullName: "Internal Auditor", username: "auditor", password: "audit123", role: "Viewer / Auditor", active: true }
  ],
  assets: [
    {
      id: "a1",
      deviceDescription: "Dell Latitude 5440 laptop",
      assetBarcode: "ICT-LAP-0001",
      serialNumber: "DL5440ZA001",
      department: "Corporate Services",
      section: "ICT Operations",
      building: "Civic Centre",
      officeNumber: "ICT-101",
      roomBarcode: "ROOM-CIVIC-ICT101",
      currentOwner: "Thabo Mokoena",
      previousOwner: "Stores",
      technician: "Thabo Mokoena",
      registeredAt: Date.now() - 7 * 86400000,
      movedAt: Date.now() - 6 * 86400000,
      movementType: "New allocation",
      notes: "Sample laptop issued to ICT technician.",
      photo: ""
    },
    {
      id: "a2",
      deviceDescription: "HP LaserJet Pro printer",
      assetBarcode: "ICT-PRN-0042",
      serialNumber: "HPLJPRO042ZA",
      department: "Finance",
      section: "Revenue",
      building: "Finance Building",
      officeNumber: "FIN-03",
      roomBarcode: "ROOM-FIN-003",
      currentOwner: "Finance Shared Office",
      previousOwner: "Stores",
      technician: "Thabo Mokoena",
      registeredAt: Date.now() - 3 * 86400000,
      movedAt: Date.now() - 2 * 86400000,
      movementType: "New allocation",
      notes: "Network printer for revenue team.",
      photo: ""
    }
  ],
  movements: [
    {
      id: "m1",
      assetId: "a1",
      assetBarcode: "ICT-LAP-0001",
      serialNumber: "DL5440ZA001",
      deviceDescription: "Dell Latitude 5440 laptop",
      previousOwner: "Stores",
      newOwner: "Thabo Mokoena",
      previousLocation: "Stores",
      newBuilding: "Civic Centre",
      newOfficeNumber: "ICT-101",
      department: "Corporate Services",
      section: "ICT Operations",
      roomBarcode: "ROOM-CIVIC-ICT101",
      movementType: "New allocation",
      reason: "Initial allocation after asset registration.",
      technician: "Thabo Mokoena",
      confirmation: "Thabo Mokoena",
      movementDate: Date.now() - 6 * 86400000
    }
  ],
  audits: []
};

let state = loadState();
let activeScreen = "dashboard";
let activeReport = null;

function loadState() {
  if (remoteMode) return { currentUser: null, users: [], assets: [], movements: [], audits: [] };
  const saved = localStorage.getItem(STORAGE_KEY);
  return saved ? JSON.parse(saved) : structuredClone(seedState);
}

function saveState() {
  if (remoteMode) return;
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

async function refreshRemoteData() {
  if (!remoteMode || !state.currentUser) return;
  const data = await remoteBackend.loadData();
  state.users = data.users;
  state.assets = data.assets;
  state.movements = data.movements;
}

function $(selector) {
  return document.querySelector(selector);
}

function $all(selector) {
  return Array.from(document.querySelectorAll(selector));
}

function text(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  }[char]));
}

function formatDate(value) {
  return new Date(value).toLocaleDateString();
}

function formatDateTime(value) {
  return new Date(value).toLocaleString();
}

function canWriteAssets() {
  return ["Admin", "Standard User", "ICT Technician"].includes(state.currentUser?.role);
}

function canManageUsers() {
  return state.currentUser?.role === "Admin";
}

async function signIn(username, password) {
  if (remoteMode) {
    try {
      state.currentUser = await remoteBackend.signIn(username, password);
      await refreshRemoteData();
      return true;
    } catch (error) {
      console.error("Sign in failed", error);
      return false;
    }
  }
  const user = state.users.find((item) => item.active && item.username === username && item.password === password);
  if (!user) return false;
  state.currentUser = { id: user.id, fullName: user.fullName, username: user.username, role: user.role };
  saveState();
  return true;
}

async function signOut() {
  if (remoteMode) await remoteBackend.signOut();
  state.currentUser = null;
  saveState();
  renderAuth();
}

function renderAuth() {
  const loggedIn = Boolean(state.currentUser);
  $("#login-screen").hidden = loggedIn;
  $("#app-shell").hidden = !loggedIn;
  if (loggedIn) {
    $("#current-user").textContent = `${state.currentUser.fullName} · ${state.currentUser.role}`;
    $all(".admin-only").forEach((node) => {
      node.hidden = !canManageUsers();
    });
    render();
  }
}

function setScreen(screen) {
  activeScreen = screen;
  $all(".screen").forEach((node) => node.classList.toggle("active-screen", node.id === screen));
  $all(".nav-item").forEach((node) => node.classList.toggle("active", node.dataset.screen === screen));
  const titles = {
    dashboard: ["Dashboard", "Municipal ICT asset overview"],
    assets: ["Assets", "Search and inspect registered assets"],
    capture: ["Capture Asset", "Manual web capture for colleagues"],
    movements: ["Movements", "Allocate, transfer, return, repair, or dispose assets"],
    reports: ["Reports", "Preview reports before downloading"],
    users: ["Users", "Admin user management"]
  };
  $("#screen-title").textContent = titles[screen]?.[0] ?? "Dashboard";
  $("#screen-subtitle").textContent = titles[screen]?.[1] ?? "";
  render();
}

function assetMatches(asset, query) {
  const haystack = [asset.assetBarcode, asset.serialNumber].join(" ").toLowerCase();
  return haystack.includes(query.toLowerCase());
}

function render() {
  if (!state.currentUser) return;
  renderDashboard();
  renderAssets();
  renderMovements();
  renderUsers();
  updateAssetDatalist();
}

function renderDashboard() {
  $("#metric-total").textContent = state.assets.length;
  $("#metric-moved").textContent = state.movements.length;
  $("#metric-allocated").textContent = state.assets.filter((asset) => asset.currentOwner.trim()).length;
  $("#metric-recent").textContent = state.movements.slice(0, 10).length;

  $("#dashboard-assets").innerHTML = state.assets.slice(0, 8).map((asset) => `
    <tr>
      <td>${text(asset.assetBarcode)}</td>
      <td>${text(asset.deviceDescription)}</td>
      <td>${text(asset.currentOwner)}</td>
      <td>${text(asset.department)}</td>
      <td>${text(asset.building)} ${text(asset.officeNumber)}</td>
    </tr>
  `).join("");

  $("#recent-movements").innerHTML = state.movements.slice(0, 6).map(movementCard).join("") || emptyCard("No movements recorded.");
}

function renderStatus(type) {
  const status = $("#dashboard-status");
  status.hidden = false;
  const list = $("#status-list");
  const map = {
    all: ["Total assets", state.assets.map(assetCard)],
    allocated: ["Allocated assets", state.assets.filter((asset) => asset.currentOwner.trim()).map(assetCard)],
    moved: ["Moved assets", state.movements.map(movementCard)],
    recent: ["Recent moves", state.movements.slice(0, 10).map(movementCard)]
  };
  $("#status-title").textContent = map[type][0];
  list.innerHTML = map[type][1].join("") || emptyCard("No records found.");
}

function renderAssets() {
  const query = $("#asset-search").value.trim();
  const assets = query ? state.assets.filter((asset) => assetMatches(asset, query)) : [];
  $("#asset-list").innerHTML = assets.map(assetCard).join("") ||
    emptyCard(query ? "No matching barcode or serial number found." : "Enter a barcode or serial number to find one device.");
}

function renderMovements() {
  $("#movement-list").innerHTML = state.movements.map(movementCard).join("") || emptyCard("No movements recorded.");
}

function renderUsers() {
  const list = $("#user-list");
  if (!canManageUsers()) {
    list.innerHTML = emptyCard("Admin access required.");
    return;
  }
  list.innerHTML = state.users.filter((user) => user.active).map((user) => `
    <div class="user-card">
      <div>
        <strong>${text(user.fullName)}</strong>
        <p>${text(user.username)} · ${text(user.role)}</p>
      </div>
      <button class="danger-btn" data-delete-user="${text(user.id)}" ${user.id === state.currentUser.id ? "disabled" : ""}>Delete</button>
    </div>
  `).join("");
}

function assetCard(asset) {
  return `
    <button type="button" class="asset-card asset-card-button" data-open-asset="${text(asset.id)}">
      <div class="asset-card-header">
        <div>
          <h4>${text(asset.deviceDescription)}</h4>
          <p>${text(asset.assetBarcode)} · ${text(asset.serialNumber)}</p>
        </div>
        <span class="status-chip">${text(asset.movementType)}</span>
      </div>
      <p>Owner: ${text(asset.currentOwner || "Unassigned")}</p>
      <p>Department: ${text(asset.department)} / ${text(asset.section || "-")}</p>
      <p>Location: ${text(asset.building)} · ${text(asset.officeNumber)} · ${text(asset.roomBarcode || "-")}</p>
      <p>Registered: ${formatDate(asset.registeredAt)}</p>
      <p class="open-device-hint">Open device details${asset.photoPath || asset.photo ? " and photo" : ""}</p>
    </button>
  `;
}

async function openAssetDetail(assetId) {
  const asset = state.assets.find((item) => item.id === assetId);
  if (!asset) return;
  const content = $("#asset-detail-content");
  content.innerHTML = `<p>Loading device...</p>`;
  $("#asset-detail-dialog").showModal();
  let photoUrl = asset.photo || "";
  if (remoteMode && asset.photoPath) {
    try {
      photoUrl = await remoteBackend.photoUrl(asset.photoPath);
    } catch (error) {
      console.error("Unable to load asset photo", error);
    }
  }
  content.innerHTML = `
    <div class="asset-detail-grid">
      <p><strong>Device</strong><span>${text(asset.deviceDescription)}</span></p>
      <p><strong>Asset barcode</strong><span>${text(asset.assetBarcode)}</span></p>
      <p><strong>Serial number</strong><span>${text(asset.serialNumber)}</span></p>
      <p><strong>Owner</strong><span>${text(asset.currentOwner || "Unassigned")}</span></p>
      <p><strong>Department</strong><span>${text(asset.department)} / ${text(asset.section || "-")}</span></p>
      <p><strong>Location</strong><span>${text(asset.building)} · ${text(asset.officeNumber)} · ${text(asset.roomBarcode || "-")}</span></p>
      <p><strong>Technician</strong><span>${text(asset.technician)}</span></p>
      <p><strong>Movement type</strong><span>${text(asset.movementType)}</span></p>
      <p><strong>Notes</strong><span>${text(asset.notes || "-")}</span></p>
    </div>
    ${photoUrl ? `<img class="asset-detail-photo" src="${text(photoUrl)}" alt="Photo of ${text(asset.assetBarcode)}" />` : `<p class="no-photo">No photo is stored for this device.</p>`}
  `;
}

async function scanSerialImage(file) {
  if (!file) return;
  const message = $("#asset-form-message");
  if (!("BarcodeDetector" in window)) {
    message.textContent = "Serial scanning is not supported by this browser. Please type the serial number.";
    return;
  }
  try {
    const detector = new BarcodeDetector();
    const bitmap = await createImageBitmap(file);
    const codes = await detector.detect(bitmap);
    bitmap.close();
    const value = codes[0]?.rawValue?.trim();
    if (!value) throw new Error("No barcode was detected on the serial label.");
    $("#asset-form").elements.serialNumber.value = value.toUpperCase();
    message.textContent = "Serial number scanned. Please verify it before saving.";
  } catch (error) {
    message.textContent = error.message || "Unable to scan the serial label.";
  }
}

function movementCard(movement) {
  return `
    <article class="record-card">
      <div class="record-card-header">
        <div>
          <h4>${text(movement.assetBarcode)} · ${text(movement.deviceDescription)}</h4>
          <p>${text(movement.previousOwner)} to ${text(movement.newOwner)}</p>
        </div>
        <span class="status-chip">${text(movement.movementType)}</span>
      </div>
      <p>Location: ${text(movement.newBuilding)} · ${text(movement.newOfficeNumber)}</p>
      <p>Technician: ${text(movement.technician)} · ${formatDateTime(movement.movementDate)}</p>
      <p>Reason: ${text(movement.reason)}</p>
    </article>
  `;
}

function emptyCard(message) {
  return `<div class="record-card"><p>${text(message)}</p></div>`;
}

function updateAssetDatalist() {
  $("#asset-barcodes").innerHTML = state.assets.map((asset) => `<option value="${text(asset.assetBarcode)}"></option>`).join("");
}

async function fileToDataUrl(file) {
  if (!file) return "";
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

function formObject(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function handleAssetSubmit(event) {
  event.preventDefault();
  if (!canWriteAssets()) {
    $("#asset-form-message").textContent = "Your role cannot capture assets.";
    return;
  }
  const form = event.currentTarget;
  const data = formObject(form);
  const barcode = data.assetBarcode.trim().toUpperCase();
  const serial = data.serialNumber.trim().toUpperCase();
  if (state.assets.some((asset) => asset.assetBarcode === barcode)) {
    $("#asset-form-message").textContent = "Asset barcode already exists.";
    return;
  }
  if (state.assets.some((asset) => asset.serialNumber === serial)) {
    $("#asset-form-message").textContent = "Serial number already exists.";
    return;
  }
  if (remoteMode) {
    try {
      await remoteBackend.createAsset(data, form.assetPhoto.files[0], state.currentUser);
      await refreshRemoteData();
      form.reset();
      $("#asset-form-message").textContent = "Asset saved to the shared database.";
      render();
    } catch (error) {
      $("#asset-form-message").textContent = error.message || "Unable to save the asset.";
    }
    return;
  }
  const photo = await fileToDataUrl(form.assetPhoto.files[0]);
  const asset = {
    id: crypto.randomUUID(),
    deviceDescription: data.deviceDescription.trim(),
    assetBarcode: barcode,
    serialNumber: serial,
    department: data.department.trim(),
    section: data.section.trim(),
    building: data.building.trim(),
    officeNumber: data.officeNumber.trim(),
    roomBarcode: data.roomBarcode.trim().toUpperCase(),
    currentOwner: data.currentOwner.trim(),
    previousOwner: data.previousOwner.trim(),
    technician: data.technician.trim(),
    registeredAt: Date.now(),
    movedAt: Date.now(),
    movementType: data.movementType,
    notes: data.notes.trim(),
    photo
  };
  state.assets.unshift(asset);
  state.movements.unshift({
    id: crypto.randomUUID(),
    assetId: asset.id,
    assetBarcode: asset.assetBarcode,
    serialNumber: asset.serialNumber,
    deviceDescription: asset.deviceDescription,
    previousOwner: asset.previousOwner || "Stores",
    newOwner: asset.currentOwner,
    previousLocation: "Stores",
    newBuilding: asset.building,
    newOfficeNumber: asset.officeNumber,
    department: asset.department,
    section: asset.section,
    roomBarcode: asset.roomBarcode,
    movementType: "New allocation",
    reason: "Asset manually captured on web.",
    technician: asset.technician,
    confirmation: state.currentUser.fullName,
    movementDate: Date.now()
  });
  saveState();
  form.reset();
  $("#asset-form-message").textContent = "Asset saved.";
  render();
}

async function handleMovementSubmit(event) {
  event.preventDefault();
  if (!canWriteAssets()) {
    $("#movement-form-message").textContent = "Your role cannot move assets.";
    return;
  }
  const form = event.currentTarget;
  const data = formObject(form);
  const barcode = data.assetBarcode.trim().toUpperCase();
  const asset = state.assets.find((item) => item.assetBarcode === barcode);
  if (!asset) {
    $("#movement-form-message").textContent = "Asset barcode not found.";
    return;
  }
  if (remoteMode) {
    try {
      await remoteBackend.recordMovement(asset.id, data);
      await refreshRemoteData();
      form.reset();
      $("#movement-form-message").textContent = "Movement saved to the shared database.";
      render();
    } catch (error) {
      $("#movement-form-message").textContent = error.message || "Unable to record the movement.";
    }
    return;
  }
  const previousLocation = `${asset.building} / ${asset.officeNumber} / ${asset.roomBarcode || "-"}`;
  const movement = {
    id: crypto.randomUUID(),
    assetId: asset.id,
    assetBarcode: asset.assetBarcode,
    serialNumber: asset.serialNumber,
    deviceDescription: asset.deviceDescription,
    previousOwner: asset.currentOwner,
    newOwner: data.newOwner.trim(),
    previousLocation,
    newBuilding: data.newBuilding.trim(),
    newOfficeNumber: data.newOfficeNumber.trim(),
    department: data.department.trim(),
    section: data.section.trim(),
    roomBarcode: data.roomBarcode.trim().toUpperCase(),
    movementType: data.movementType,
    reason: data.reason.trim(),
    technician: data.technician.trim(),
    confirmation: data.confirmation.trim(),
    movementDate: Date.now()
  };
  Object.assign(asset, {
    previousOwner: asset.currentOwner,
    currentOwner: movement.newOwner,
    building: movement.newBuilding,
    officeNumber: movement.newOfficeNumber,
    department: movement.department,
    section: movement.section,
    roomBarcode: movement.roomBarcode,
    technician: movement.technician,
    movedAt: movement.movementDate,
    movementType: movement.movementType
  });
  state.movements.unshift(movement);
  saveState();
  form.reset();
  $("#movement-form-message").textContent = "Movement recorded.";
  render();
}

function buildReport() {
  const type = $("#report-type").value;
  const filter = $("#report-filter").value.trim().toLowerCase();
  const groupCount = (items, key) => {
    const map = new Map();
    items.forEach((item) => {
      const value = key(item) || "Unassigned";
      map.set(value, (map.get(value) || 0) + 1);
    });
    return Array.from(map.entries()).map(([name, total]) => [name, total]);
  };
  if (type === "complete") {
    const fieldsFor = (asset) => [
      asset.id,
      asset.deviceDescription,
      asset.assetBarcode,
      asset.serialNumber,
      asset.department,
      asset.section,
      asset.building,
      asset.officeNumber,
      asset.roomBarcode,
      asset.currentOwner,
      asset.previousOwner,
      asset.technician,
      asset.registeredAt,
      asset.movedAt,
      asset.movementType,
      asset.notes
    ];
    const assets = state.assets.filter((asset) =>
      !filter || fieldsFor(asset).some((value) => String(value ?? "").toLowerCase().includes(filter))
    );
    return {
      title: `Complete asset register (${assets.length} asset${assets.length === 1 ? "" : "s"})`,
      headers: [
        "Asset ID",
        "Device description",
        "Asset barcode",
        "Serial number",
        "Department",
        "Section",
        "Building",
        "Office number",
        "Room barcode",
        "Current owner",
        "Previous owner",
        "Technician",
        "Date registered",
        "Date moved",
        "Movement type",
        "Notes"
      ],
      rows: assets.map((asset) => [
        asset.id,
        asset.deviceDescription,
        asset.assetBarcode,
        asset.serialNumber,
        asset.department,
        asset.section,
        asset.building,
        asset.officeNumber,
        asset.roomBarcode,
        asset.currentOwner,
        asset.previousOwner,
        asset.technician,
        formatDateTime(asset.registeredAt),
        asset.movedAt ? formatDateTime(asset.movedAt) : "",
        asset.movementType,
        asset.notes
      ])
    };
  }
  if (type === "department") return { title: "Assets per department", headers: ["Department", "Total"], rows: groupCount(state.assets, (a) => a.department) };
  if (type === "building") return { title: "Assets per building", headers: ["Building", "Total"], rows: groupCount(state.assets, (a) => a.building) };
  if (type === "owner") {
    const assets = state.assets.filter((asset) => !filter || asset.currentOwner.toLowerCase().includes(filter));
    return { title: "Assets allocated to user", headers: ["Owner", "Total"], rows: groupCount(assets, (a) => a.currentOwner) };
  }
  if (type === "technician") {
    const moves = state.movements.filter((movement) => !filter || movement.technician.toLowerCase().includes(filter));
    return { title: "Assets moved by technician", headers: ["Technician", "Total"], rows: groupCount(moves, (m) => m.technician) };
  }
  return {
    title: "Asset movement history",
    headers: ["Barcode", "Device", "Previous owner", "New owner", "Technician", "Date"],
    rows: state.movements.map((movement) => [
      movement.assetBarcode,
      movement.deviceDescription,
      movement.previousOwner,
      movement.newOwner,
      movement.technician,
      formatDateTime(movement.movementDate)
    ])
  };
}

function renderReport() {
  activeReport = buildReport();
  $("#report-title").textContent = activeReport.title;
  $("#download-report").disabled = false;
  $("#report-table").innerHTML = `
    <thead><tr>${activeReport.headers.map((header) => `<th>${text(header)}</th>`).join("")}</tr></thead>
    <tbody>${activeReport.rows.map((row) => `<tr>${row.map((cell) => `<td>${text(cell)}</td>`).join("")}</tr>`).join("")}</tbody>
  `;
}

function downloadReport() {
  if (!activeReport) return;
  const rows = [activeReport.headers, ...activeReport.rows];
  const csv = rows.map((row) => row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${activeReport.title.toLowerCase().replace(/[^a-z0-9]+/g, "_")}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function handleUserSubmit(event) {
  event.preventDefault();
  if (!canManageUsers()) return;
  if (remoteMode) {
    $("#user-form-message").textContent = "Production users must currently be created in Supabase Authentication.";
    return;
  }
  const form = event.currentTarget;
  const data = formObject(form);
  const username = data.username.trim().toLowerCase();
  if (state.users.some((user) => user.active && user.username === username)) {
    $("#user-form-message").textContent = "Username already exists.";
    return;
  }
  state.users.push({
    id: crypto.randomUUID(),
    fullName: data.fullName.trim(),
    username,
    password: data.password.trim(),
    role: data.role,
    active: true
  });
  saveState();
  form.reset();
  $("#user-form-message").textContent = "User created.";
  renderUsers();
}

function deleteUser(userId) {
  if (!canManageUsers() || userId === state.currentUser.id) return;
  if (remoteMode) return;
  const user = state.users.find((item) => item.id === userId);
  if (!user) return;
  user.active = false;
  saveState();
  renderUsers();
}

function bindEvents() {
  $("#login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const submitButton = event.currentTarget.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    const ok = await signIn($("#login-username").value.trim().toLowerCase(), $("#login-password").value);
    submitButton.disabled = false;
    $("#login-error").textContent = ok ? "" : "Invalid username or password.";
    $("#login-error").classList.toggle("error-text", !ok);
    if (ok) renderAuth();
  });
  $("#logout-btn").addEventListener("click", signOut);
  $all(".nav-item").forEach((button) => button.addEventListener("click", () => setScreen(button.dataset.screen)));
  $all("[data-screen-link]").forEach((button) => button.addEventListener("click", () => setScreen(button.dataset.screenLink)));
  $all(".metric-card").forEach((button) => button.addEventListener("click", () => renderStatus(button.dataset.status)));
  $("#close-status").addEventListener("click", () => {
    $("#dashboard-status").hidden = true;
  });
  $("#asset-search").addEventListener("input", renderAssets);
  $("#clear-search").addEventListener("click", () => {
    $("#asset-search").value = "";
    renderAssets();
  });
  $("#asset-form").addEventListener("submit", handleAssetSubmit);
  $("#scan-serial").addEventListener("click", () => $("#serial-scan-image").click());
  $("#serial-scan-image").addEventListener("change", (event) => scanSerialImage(event.target.files[0]));
  $("#close-asset-detail").addEventListener("click", () => $("#asset-detail-dialog").close());
  $("#movement-form").addEventListener("submit", handleMovementSubmit);
  $("#load-report").addEventListener("click", renderReport);
  $("#download-report").addEventListener("click", downloadReport);
  $("#user-form").addEventListener("submit", handleUserSubmit);
  document.addEventListener("click", (event) => {
    const deleteButton = event.target.closest("[data-delete-user]");
    if (deleteButton) deleteUser(deleteButton.dataset.deleteUser);
    const assetButton = event.target.closest("[data-open-asset]");
    if (assetButton) openAssetDetail(assetButton.dataset.openAsset);
  });
}

bindEvents();

async function initializeApp() {
  if (remoteMode) {
    try {
      state.currentUser = await remoteBackend.currentUser();
      if (state.currentUser) await refreshRemoteData();
    } catch (error) {
      console.error("Unable to restore session", error);
      state.currentUser = null;
    }
  }
  renderAuth();
}

initializeApp();
