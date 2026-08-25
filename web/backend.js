(function () {
  const config = window.ICT_ASSET_CONFIG || {};
  const enabled = Boolean(config.supabaseUrl && config.supabaseAnonKey && window.supabase);
  const client = enabled ? window.supabase.createClient(config.supabaseUrl, config.supabaseAnonKey) : null;

  const roleLabels = {
    admin: "Admin",
    standard_user: "Standard User",
    ict_technician: "ICT Technician",
    viewer_auditor: "Viewer / Auditor"
  };

  const movementLabels = {
    new_allocation: "New allocation",
    transfer: "Transfer",
    return: "Return",
    repair: "Repair",
    disposal: "Disposal"
  };

  const movementValues = Object.fromEntries(Object.entries(movementLabels).map(([key, value]) => [value, key]));

  function profileToUser(profile) {
    return {
      id: profile.id,
      fullName: profile.full_name,
      username: profile.username,
      role: roleLabels[profile.role] || profile.role,
      active: profile.active
    };
  }

  function rowToAsset(row) {
    return {
      id: row.id,
      deviceDescription: row.device_description,
      assetBarcode: row.asset_barcode,
      serialNumber: row.serial_number,
      department: row.department,
      section: row.section,
      building: row.building,
      officeNumber: row.office_number,
      roomBarcode: row.room_barcode,
      currentOwner: row.current_owner,
      previousOwner: row.previous_owner,
      technician: row.technician,
      registeredAt: row.registered_at,
      movedAt: row.moved_at,
      movementType: movementLabels[row.movement_type] || row.movement_type,
      notes: row.notes,
      photoPath: row.photo_path || "",
      photo: ""
    };
  }

  function rowToMovement(row, asset) {
    return {
      id: row.id,
      assetId: row.asset_id,
      assetBarcode: asset?.assetBarcode || "",
      serialNumber: asset?.serialNumber || "",
      deviceDescription: asset?.deviceDescription || "",
      previousOwner: row.previous_owner,
      newOwner: row.new_owner,
      previousLocation: row.previous_location,
      newBuilding: row.new_building,
      newOfficeNumber: row.new_office_number,
      department: row.department,
      section: row.section,
      roomBarcode: row.room_barcode,
      movementType: movementLabels[row.movement_type] || row.movement_type,
      reason: row.reason,
      technician: row.technician,
      confirmation: row.confirmation,
      movementDate: row.movement_date
    };
  }

  async function currentUser() {
    const { data: { session }, error: sessionError } = await client.auth.getSession();
    if (sessionError) throw sessionError;
    if (!session) return null;
    const { data, error } = await client.from("profiles").select("*").eq("id", session.user.id).single();
    if (error) throw error;
    if (!data.active) throw new Error("This account is disabled.");
    return profileToUser(data);
  }

  async function signIn(login, password) {
    const email = login.includes("@") ? login : `${login}@ict-register.local`;
    const { error } = await client.auth.signInWithPassword({ email, password });
    if (error) throw error;
    return currentUser();
  }

  async function loadData() {
    const [assetsResult, movementsResult, profilesResult] = await Promise.all([
      client.from("assets").select("*").order("registered_at", { ascending: false }),
      client.from("asset_movements").select("*").order("movement_date", { ascending: false }),
      client.from("profiles").select("*").order("full_name"),
    ]);
    for (const result of [assetsResult, movementsResult, profilesResult]) {
      if (result.error) throw result.error;
    }
    const assets = assetsResult.data.map(rowToAsset);
    const assetsById = new Map(assets.map((asset) => [asset.id, asset]));
    return {
      assets,
      movements: movementsResult.data.map((row) => rowToMovement(row, assetsById.get(row.asset_id))),
      users: profilesResult.data.map(profileToUser)
    };
  }

  async function uploadPhoto(file, assetId) {
    if (!file) return null;
    const compressed = await compressPhoto(file);
    const path = `${assetId}/${crypto.randomUUID()}.jpg`;
    const { error } = await client.storage.from("asset-photos").upload(path, compressed, {
      contentType: "image/jpeg",
      upsert: false
    });
    if (error) throw error;
    return path;
  }

  async function compressPhoto(file) {
    const bitmap = await createImageBitmap(file);
    const longestSide = Math.max(bitmap.width, bitmap.height);
    const scale = Math.min(1, 960 / longestSide);
    const canvas = document.createElement("canvas");
    canvas.width = Math.max(1, Math.round(bitmap.width * scale));
    canvas.height = Math.max(1, Math.round(bitmap.height * scale));
    canvas.getContext("2d").drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    bitmap.close();
    return new Promise((resolve, reject) => canvas.toBlob(
      (blob) => blob ? resolve(blob) : reject(new Error("Unable to compress asset photo.")),
      "image/jpeg",
      0.48
    ));
  }

  async function photoUrl(path) {
    if (!path) return "";
    const { data, error } = await client.storage.from("asset-photos").createSignedUrl(path, 300);
    if (error) throw error;
    return data.signedUrl;
  }

  async function createAsset(data, photoFile, user) {
    const id = crypto.randomUUID();
    const photoPath = await uploadPhoto(photoFile, id);
    const payload = {
      id,
      device_description: data.deviceDescription.trim(),
      asset_barcode: data.assetBarcode.trim().toUpperCase(),
      serial_number: data.serialNumber.trim().toUpperCase(),
      department: data.department.trim(),
      section: data.section.trim(),
      building: data.building.trim(),
      office_number: data.officeNumber.trim(),
      room_barcode: data.roomBarcode.trim().toUpperCase(),
      current_owner: data.currentOwner.trim(),
      previous_owner: data.previousOwner.trim(),
      technician: data.technician.trim(),
      movement_type: movementValues[data.movementType],
      notes: data.notes.trim(),
      photo_path: photoPath,
      created_by: user.id,
      updated_by: user.id
    };
    const { data: row, error } = await client.from("assets").insert(payload).select().single();
    if (error) throw error;
    const { error: movementError } = await client.from("asset_movements").insert({
      asset_id: row.id,
      previous_owner: row.previous_owner || "Stores",
      new_owner: row.current_owner,
      previous_location: "Stores",
      new_building: row.building,
      new_office_number: row.office_number,
      department: row.department,
      section: row.section,
      room_barcode: row.room_barcode,
      movement_type: "new_allocation",
      reason: "Asset manually captured on web.",
      technician: row.technician,
      confirmation: user.fullName,
      created_by: user.id
    });
    if (movementError) throw movementError;
  }

  async function recordMovement(assetId, data) {
    const { error } = await client.rpc("record_asset_movement", {
      p_asset_id: assetId,
      p_new_owner: data.newOwner.trim(),
      p_new_building: data.newBuilding.trim(),
      p_new_office_number: data.newOfficeNumber.trim(),
      p_department: data.department.trim(),
      p_section: data.section.trim(),
      p_room_barcode: data.roomBarcode.trim().toUpperCase(),
      p_movement_type: movementValues[data.movementType],
      p_reason: data.reason.trim(),
      p_technician: data.technician.trim(),
      p_confirmation: data.confirmation.trim()
    });
    if (error) throw error;
  }

  window.assetBackend = {
    enabled,
    currentUser,
    signIn,
    photoUrl,
    signOut: async () => {
      const { error } = await client.auth.signOut();
      if (error) throw error;
    },
    loadData,
    createAsset,
    recordMovement
  };
})();
