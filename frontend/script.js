/* =========================================================
   FUNDHUB - FRONTEND CLIENT
   Handles DOM rendering, user events, and Java API calls.
   All business logic and database writes run in Java backend.
========================================================= */

import { initializeApp } from "https://www.gstatic.com/firebasejs/12.3.0/firebase-app.js";
import {
    browserSessionPersistence,
    createUserWithEmailAndPassword,
    deleteUser,
    getAuth,
    onAuthStateChanged,
    setPersistence,
    signInWithEmailAndPassword,
    signOut
} from "https://www.gstatic.com/firebasejs/12.3.0/firebase-auth.js";
import {
    collection,
    doc,
    getDoc,
    getFirestore,
    onSnapshot,
    query,
    where
} from "https://www.gstatic.com/firebasejs/12.3.0/firebase-firestore.js";
import { API_BASE_URL, CURRENCY, LOCALE } from "./config.js";

/* --- FIREBASE INITIALIZATION --- */
const firebaseConfig = {
    apiKey: "AIzaSyBYiijyaBxtS_HkviqBzSEGTKI2m7vbtuI",
    authDomain: "crowdingfunding.firebaseapp.com",
    databaseURL: "https://crowdingfunding-default-rtdb.firebaseio.com",
    projectId: "crowdingfunding",
    storageBucket: "crowdingfunding.firebasestorage.app",
    messagingSenderId: "394070986612",
    appId: "1:394070986612:web:393006f052981bf9dba621",
    measurementId: "G-8LTEXQZMXS"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

setPersistence(auth, browserSessionPersistence).catch(console.error);

/* --- APPLICATION STATE --- */
const VALID_ROLES = ["ADMIN", "CREATOR", "CONTRIBUTOR"];
let currentUser = null;
let currentUserData = null;
let currentRole = null;
let campaigns = [];
let users = [];
let contributions = [];
let listeners = [];
let editingUserId = null;
let registering = false;
let busy = false;

/* --- DOM REFERENCES --- */
const $ = (id) => document.getElementById(id);
const loginPage = $("loginPage");
const appPage = $("appPage");
const loginPanel = $("loginPanel");
const registerPanel = $("registerPanel");
const loginForm = $("loginForm");
const registerForm = $("registerForm");
const loginError = $("loginError");
const registerError = $("registerError");
const loginButton = $("loginButton");
const registerButton = $("registerButton");
const logoutButton = $("logoutButton");
const campaignForm = $("campaignForm");
const campaignMessage = $("campaignMessage");
const createCampaignButton = $("createCampaignButton");
const roleModal = $("roleModal");
const editUserRole = $("editUserRole");
const saveRoleButton = $("saveRoleButton");
const dashboards = {
    ADMIN: $("adminDashboard"),
    CREATOR: $("creatorDashboard"),
    CONTRIBUTOR: $("contributorDashboard")
};

/* --- PRESENTATION HELPERS --- */
function escapeHTML(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function formatCurrency(amount) {
    return new Intl.NumberFormat(LOCALE, {
        style: "currency",
        currency: CURRENCY,
        minimumFractionDigits: 2
    }).format(Number(amount) || 0);
}

function getTimestampMillis(ts) {
    if (!ts) return 0;
    if (typeof ts.toMillis === "function") return ts.toMillis();
    if (ts.seconds !== undefined) return ts.seconds * 1000;
    if (ts instanceof Date) return ts.getTime();
    return 0;
}

function formatTimestamp(ts) {
    const millis = getTimestampMillis(ts);
    if (!millis) return "Just now";
    return new Date(millis).toLocaleString(LOCALE, {
        day: "numeric", month: "short", year: "numeric",
        hour: "numeric", minute: "2-digit"
    });
}

function statusOf(c) {
    return String(c.status || "PENDING").toUpperCase();
}

function getStatusClass(status) {
    switch (String(status || "").toUpperCase()) {
        case "APPROVED": return "status-approved";
        case "REJECTED": return "status-rejected";
        case "COMPLETED": return "status-completed";
        default: return "status-pending";
    }
}

function getRoleLabel(role) {
    switch (role) {
        case "ADMIN": return "Admin";
        case "CREATOR": return "Creator";
        case "CONTRIBUTOR": return "Contributor";
        default: return role || "Unknown";
    }
}

function progressOf(c) {
    const target = Number(c.targetAmount) || 0;
    const collected = Number(c.collectedAmount) || 0;
    const percentage = target > 0 ? Math.min(100, Math.floor((collected / target) * 100)) : 0;
    return { target, collected, percentage };
}

function progressBar(percentage) {
    return `
        <div class="campaign-progress" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${percentage}">
            <div class="campaign-progress-track">
                <div class="campaign-progress-fill" style="width: ${percentage}%"></div>
            </div>
        </div>
    `;
}

function statCard(label, value) {
    return `
        <div class="stat-card">
            <div class="stat-value">${escapeHTML(value)}</div>
            <div class="stat-label">${escapeHTML(label)}</div>
        </div>
    `;
}

function matchesSearch(item, text, fields) {
    if (!text) return true;
    return fields.map((f) => String(item[f] || "").toLowerCase()).join(" ").includes(text);
}

function sortCampaigns(list, pendingFirst = false) {
    const rank = (c) => {
        const s = statusOf(c);
        if (pendingFirst && s === "PENDING") return 0;
        return s === "COMPLETED" ? 2 : 1;
    };
    return [...list].sort((a, b) => {
        const diff = rank(a) - rank(b);
        if (diff !== 0) return diff;
        return getTimestampMillis(b.createdAt) - getTimestampMillis(a.createdAt);
    });
}

/* --- UI FEEDBACK & DIALOGS --- */
function showLoading(message = "Loading...") {
    $("loadingText").textContent = message;
    $("loadingOverlay").classList.remove("hidden");
}

function hideLoading() {
    $("loadingOverlay").classList.add("hidden");
}

function setButtonLoading(button, loading, label = "Loading...") {
    if (!button) return;
    if (loading) {
        if (button.dataset.isLoading === "true") return;
        button.dataset.originalText = button.innerHTML;
        button.dataset.isLoading = "true";
        button.disabled = true;
        button.innerHTML = `<span class="button-spinner"></span>${escapeHTML(label)}`;
        return;
    }
    button.disabled = false;
    if (button.dataset.isLoading === "true") {
        button.innerHTML = button.dataset.originalText || button.innerHTML;
    }
    button.dataset.isLoading = "false";
}

function showToast(message, type = "success") {
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    $("toastContainer").appendChild(toast);
    setTimeout(() => toast.remove(), 4500);
}

async function runAction(loadingMessage, task, successMessage) {
    if (busy) return false;
    busy = true;
    showLoading(loadingMessage);
    try {
        await task();
        if (successMessage) showToast(successMessage);
        return true;
    } catch (error) {
        console.error("Action failed:", error);
        showToast(error.message || "Something went wrong. Please try again.", "error");
        return false;
    } finally {
        busy = false;
        hideLoading();
    }
}

function openDialog({ title, message = "", confirmText = "Confirm", field = null, label = "", placeholder = "", validate = null }) {
    return new Promise((resolve) => {
        const modal = $("dialogModal");
        const wrap = $("dialogFieldWrap");
        const input = $("dialogInput");
        const textarea = $("dialogTextarea");
        const error = $("dialogError");
        const confirmButton = $("dialogConfirm");

        $("dialogTitle").textContent = title;
        $("dialogMessage").textContent = message;
        $("dialogFieldLabel").textContent = label;
        confirmButton.textContent = confirmText;
        error.textContent = "";

        input.value = "";
        textarea.value = "";
        wrap.classList.toggle("hidden", !field);
        input.classList.toggle("hidden", field === "textarea");
        textarea.classList.toggle("hidden", field !== "textarea");

        const active = field === "textarea" ? textarea : input;
        $("dialogFieldLabel").setAttribute("for", active.id);

        if (field === "number") {
            input.type = "number";
            input.step = "0.01";
            input.min = "0";
        } else {
            input.type = "text";
            input.removeAttribute("step");
            input.removeAttribute("min");
        }
        input.placeholder = placeholder;
        textarea.placeholder = placeholder;

        const close = (result) => {
            modal.classList.add("hidden");
            document.removeEventListener("keydown", onKey);
            resolve(result);
        };

        const submit = () => {
            const value = field ? active.value.trim() : true;
            if (field && validate) {
                const problem = validate(value);
                if (problem) {
                    error.textContent = problem;
                    return;
                }
            }
            close(value);
        };

        const onKey = (event) => {
            if (event.key === "Escape") close(null);
            if (event.key === "Enter" && field !== "textarea") {
                event.preventDefault();
                submit();
            }
        };

        confirmButton.onclick = submit;
        $("dialogCancel").onclick = () => close(null);
        modal.onclick = (event) => { if (event.target === modal) close(null); };

        document.addEventListener("keydown", onKey);
        modal.classList.remove("hidden");
        (field ? active : confirmButton).focus();
    });
}

/* --- JAVA BACKEND API CLIENT --- */
async function apiRequest(path, { method = "GET", body } = {}) {
    const user = auth.currentUser;
    if (!user) throw new Error("You are not signed in.");
    const token = await user.getIdToken();

    let response;
    try {
        response = await fetch(`${API_BASE_URL}${path}`, {
            method,
            headers: {
                Authorization: `Bearer ${token}`,
                ...(body ? { "Content-Type": "application/json" } : {})
            },
            body: body ? JSON.stringify(body) : undefined
        });
    } catch (error) {
        console.error("Network error:", error);
        throw new Error("Cannot reach the server. Check your connection or try again in a moment.");
    }

    let data = null;
    try { data = await response.json(); } catch { data = null; }
    if (!response.ok) {
        throw new Error(data?.error || `Request failed (${response.status}).`);
    }
    return data;
}

/* --- LOGIN / REGISTRATION --- */
function showPanel(which) {
    loginPanel.classList.toggle("hidden", which !== "login");
    registerPanel.classList.toggle("hidden", which !== "register");
    loginError.textContent = "";
    registerError.textContent = "";
}

$("showRegister").addEventListener("click", () => showPanel("register"));
$("showLogin").addEventListener("click", () => showPanel("login"));

function authErrorMessage(error, fallback) {
    switch (error.code) {
        case "auth/user-not-found":
        case "auth/wrong-password":
        case "auth/invalid-credential":
            return "Invalid email or password.";
        case "auth/too-many-requests":
            return "Too many attempts. Please try again later.";
        case "auth/network-request-failed":
            return "Network error. Please check your internet connection.";
        case "auth/email-already-in-use":
            return "An account with this email already exists.";
        case "auth/weak-password":
            return "Password must be at least 6 characters.";
        case "auth/invalid-email":
            return "Please enter a valid email address.";
        default:
            return error.message || fallback;
    }
}

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    loginError.textContent = "";
    const email = $("loginEmail").value.trim();
    const password = $("loginPassword").value;
    if (!email || !password) {
        loginError.textContent = "Please enter your email and password.";
        return;
    }
    try {
        setButtonLoading(loginButton, true, "Signing in...");
        showLoading("Signing you in...");
        await signInWithEmailAndPassword(auth, email, password);
    } catch (error) {
        console.error("Login error:", error);
        loginError.textContent = authErrorMessage(error, "Unable to sign in.");
        hideLoading();
        setButtonLoading(loginButton, false);
    }
});

registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    registerError.textContent = "";
    const name = $("registerName").value.trim();
    const email = $("registerEmail").value.trim();
    const password = $("registerPassword").value;
    const role = $("registerRole").value;

    if (!name || !email || password.length < 6) {
        registerError.textContent = "Please fill in all fields (password $\\ge$ 6 characters).";
        return;
    }
    if (role !== "CREATOR" && role !== "CONTRIBUTOR") {
        registerError.textContent = "Please choose a valid account type.";
        return;
    }

    let credential = null;
    registering = true;
    try {
        setButtonLoading(registerButton, true, "Creating account...");
        showLoading("Creating your account...");
        credential = await createUserWithEmailAndPassword(auth, email, password);

        await apiRequest("/api/register", {
            method: "POST",
            body: { name, role }
        });

        registering = false;
        registerForm.reset();
        await enterApplication(credential.user);
    } catch (error) {
        console.error("Registration error:", error);
        if (credential?.user) {
            try { await deleteUser(credential.user); } catch { await signOut(auth).catch(() => {}); }
        }
        registering = false;
        resetSession();
        registerError.textContent = authErrorMessage(error, "Unable to create the account.");
    } finally {
        registering = false;
        hideLoading();
        setButtonLoading(registerButton, false);
    }
});

logoutButton.addEventListener("click", async () => {
    if (!currentUser) return;
    try {
        setButtonLoading(logoutButton, true, "Logging out...");
        showLoading("Signing you out...");
        await signOut(auth);
    } catch (error) {
        console.error("Logout error:", error);
        hideLoading();
        setButtonLoading(logoutButton, false);
    }
});

/* --- AUTH STATE LISTENER --- */
onAuthStateChanged(auth, async (user) => {
    if (registering) return;
    if (!user) {
        resetSession();
        return;
    }
    await enterApplication(user);
});

async function enterApplication(user) {
    currentUser = user;
    try {
        showLoading("Preparing your FundHub dashboard...");
        await loadUserProfile(user.uid);
        showApplication();
        startRealtimeListeners();
    } catch (error) {
        console.error("User profile error:", error);
        showPanel("login");
        loginError.textContent = error.message || "Unable to load your user profile.";
        await signOut(auth).catch(() => {});
    } finally {
        hideLoading();
        setButtonLoading(loginButton, false);
    }
}

async function loadUserProfile(uid) {
    const snapshot = await getDoc(doc(db, "users", uid));
    if (!snapshot.exists()) {
        throw new Error("Your profile was not found. Please contact the administrator.");
    }
    currentUserData = snapshot.data();
    currentRole = String(currentUserData.ROLE || "").toUpperCase();
    if (!VALID_ROLES.includes(currentRole)) {
        throw new Error("Invalid user role. Please contact the administrator.");
    }
}

function resetSession() {
    currentUser = null;
    currentUserData = null;
    currentRole = null;
    campaigns = [];
    users = [];
    contributions = [];
    stopListeners();
    clearRenderedData();
    closeRoleEditor();
    Object.values(dashboards).forEach((d) => d.classList.add("hidden"));
    appPage.classList.add("hidden");
    loginPage.classList.remove("hidden");
    hideLoading();
    setButtonLoading(loginButton, false);
    setButtonLoading(logoutButton, false);
}

function clearRenderedData() {
    ["contributorCampaigns", "creatorCampaigns", "contributionHistory"].forEach((id) => {
        $(id).innerHTML = '<div class="loading-message">Loading...</div>';
    });
    ["adminCampaigns", "usersTableBody"].forEach((id) => {
        $(id).innerHTML = '<tr><td colspan="4" class="table-loading">Loading...</td></tr>';
    });
    ["adminStats", "creatorStats", "contributorStats"].forEach((id) => {
        $(id).innerHTML = "";
    });
}

function showApplication() {
    loginPage.classList.add("hidden");
    appPage.classList.remove("hidden");
    $("loggedInUser").textContent = `${getRoleLabel(currentRole)}: ${currentUserData?.name || currentUserData?.email || "User"}`;
    Object.entries(dashboards).forEach(([role, d]) => {
        d.classList.toggle("hidden", role !== currentRole);
    });
    const dashboard = dashboards[currentRole];
    const firstItem = dashboard.querySelector(".sidebar-item");
    switchDashboardSection(dashboard, firstItem.dataset.section);
}

/* --- NAVIGATION --- */
function switchDashboardSection(dashboard, sectionId) {
    const target = dashboard.querySelector(`#${sectionId}`);
    if (!target) return;
    dashboard.querySelectorAll(".dashboard-section").forEach((s) => s.classList.add("hidden"));
    target.classList.remove("hidden");
    dashboard.querySelectorAll(".sidebar-item").forEach((b) => {
        const active = b.dataset.section === sectionId;
        b.classList.toggle("active", active);
        if (active) b.setAttribute("aria-current", "page");
        else b.removeAttribute("aria-current");
    });
}

document.querySelectorAll(".sidebar-item").forEach((b) => {
    b.addEventListener("click", () => switchDashboardSection(b.closest(".role-dashboard"), b.dataset.section));
});

/* --- REAL-TIME LISTENERS --- */
function stopListeners() {
    listeners.forEach((u) => u());
    listeners = [];
}

function listenerFailed(label, error) {
    console.error(`${label} listener error:`, error);
    showToast(error.code === "permission-denied" ? `You do not have access to ${label}.` : `Live updates for ${label} stopped.`, "error");
}

function startRealtimeListeners() {
    stopListeners();
    let campaignQuery;
    if (currentRole === "ADMIN") {
        campaignQuery = collection(db, "campaigns");
    } else if (currentRole === "CREATOR") {
        campaignQuery = query(collection(db, "campaigns"), where("creatorId", "==", currentUser.uid));
    } else {
        campaignQuery = query(collection(db, "campaigns"), where("status", "in", ["APPROVED", "COMPLETED"]));
    }

    listeners.push(onSnapshot(campaignQuery, (snap) => {
        campaigns = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        renderAll();
    }, (err) => listenerFailed("campaigns", err)));

    if (currentRole === "ADMIN") {
        listeners.push(onSnapshot(collection(db, "users"), (snap) => {
            users = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
            renderAll();
        }, (err) => listenerFailed("users", err)));
    }

    if (currentRole === "CONTRIBUTOR") {
        listeners.push(onSnapshot(query(collection(db, "donations"), where("contributorId", "==", currentUser.uid)), (snap) => {
            contributions = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => getTimestampMillis(b.createdAt) - getTimestampMillis(a.createdAt));
            renderAll();
        }, (err) => listenerFailed("your contributions", err)));
    }
}

/* --- DASHBOARD RENDERING --- */
function renderAll() {
    if (currentRole === "ADMIN") {
        renderAdminStats();
        renderAdminCampaigns();
        renderUsers();
    } else if (currentRole === "CREATOR") {
        renderCreatorStats();
        renderCreatorCampaigns();
    } else if (currentRole === "CONTRIBUTOR") {
        renderContributorStats();
        renderContributorCampaigns();
        renderContributionHistory();
    }
}

function renderAdminStats() {
    const count = (s) => campaigns.filter((c) => statusOf(c) === s).length;
    const raised = campaigns.reduce((sum, c) => sum + (Number(c.collectedAmount) || 0), 0);
    const roleCount = (r) => users.filter((u) => String(u.ROLE || "").toUpperCase() === r).length;

    $("adminStats").innerHTML = [
        statCard("Pending approval", count("PENDING")),
        statCard("Approved campaigns", count("APPROVED")),
        statCard("Rejected campaigns", count("REJECTED")),
        statCard("Completed campaigns", count("COMPLETED")),
        statCard("Total raised", formatCurrency(raised)),
        statCard("Registered users", users.length),
        statCard("Admins", roleCount("ADMIN")),
        statCard("Creators", roleCount("CREATOR")),
        statCard("Contributors", roleCount("CONTRIBUTOR"))
    ].join("");
}

function renderCreatorStats() {
    const count = (s) => campaigns.filter((c) => statusOf(c) === s).length;
    const raised = campaigns.reduce((sum, c) => sum + (Number(c.collectedAmount) || 0), 0);

    $("creatorStats").innerHTML = [
        statCard("Total campaigns", campaigns.length),
        statCard("Waiting for approval", count("PENDING")),
        statCard("Live campaigns", count("APPROVED")),
        statCard("Fully funded", count("COMPLETED")),
        statCard("Total raised", formatCurrency(raised))
    ].join("");
}

function renderContributorStats() {
    const total = contributions.reduce((sum, item) => sum + (Number(item.amount) || 0), 0);
    const supported = new Set(contributions.map((i) => i.campaignId)).size;
    const open = campaigns.filter((c) => statusOf(c) === "APPROVED").length;

    $("contributorStats").innerHTML = [
        statCard("Campaigns open for support", open),
        statCard("Contributions made", contributions.length),
        statCard("Campaigns supported", supported),
        statCard("Total contributed", formatCurrency(total))
    ].join("");
}

/* --- CONTRIBUTOR ACTIONS & RENDERING --- */
function renderContributorCampaigns() {
    const container = $("contributorCampaigns");
    const searchText = $("contributorCampaignSearch").value.trim().toLowerCase();
    const visible = sortCampaigns(campaigns.filter((c) => ["APPROVED", "COMPLETED"].includes(statusOf(c)) && matchesSearch(c, searchText, ["title", "description", "creatorName"])));

    if (visible.length === 0) {
        container.innerHTML = `<div class="empty-message">${searchText ? "No campaigns match your search." : "No approved campaigns are available right now."}</div>`;
        return;
    }
    container.innerHTML = visible.map(renderContributorCard).join("");
}

function renderContributorCard(c) {
    const { target, collected, percentage } = progressOf(c);
    const completed = statusOf(c) === "COMPLETED" || (target > 0 && collected >= target);
    const status = completed ? "COMPLETED" : "APPROVED";

    return `
        <article class="campaign-card">
            <div class="campaign-card-content">
                <span class="creator-card-status ${getStatusClass(status)}">${status}</span>
                <h3 class="campaign-title">${escapeHTML(c.title || "Untitled campaign")}</h3>
                <p class="campaign-description">${escapeHTML(c.description || "No description available.")}</p>
                <p class="campaign-creator">By ${escapeHTML(c.creatorName || "Unknown")}</p>
                <div class="campaign-amount-row">
                    <div>
                        <span class="campaign-raised-amount">${formatCurrency(collected)}</span>
                        <span class="campaign-raised-label">raised</span>
                    </div>
                    <strong class="campaign-percentage">${percentage}%</strong>
                </div>
                ${progressBar(percentage)}
                <div class="campaign-goal-line">
                    <span>Goal</span>
                    <strong>${formatCurrency(target)}</strong>
                </div>
                <button type="button" class="support-button" data-action="support" data-id="${escapeHTML(c.id)}" ${completed ? "disabled" : ""}>
                    ${completed ? "Goal Reached" : "Demo Contribution"}
                </button>
            </div>
        </article>
    `;
}

function renderContributionHistory() {
    const container = $("contributionHistory");
    if (contributions.length === 0) {
        container.innerHTML = `<div class="empty-message">You haven't made any demo contributions yet. Browse approved campaigns to get started.</div>`;
        return;
    }
    container.innerHTML = contributions.map((item) => {
        const c = campaigns.find((e) => e.id === item.campaignId);
        const title = c?.title || item.campaignTitle || "Campaign";
        let progressText = "";
        if (c) {
            const { target, collected, percentage } = progressOf(c);
            progressText = `<div class="history-meta">Campaign progress: ${formatCurrency(collected)} of ${formatCurrency(target)} (${percentage}%), ${escapeHTML(statusOf(c).toLowerCase())}</div>`;
        }
        return `
            <div class="history-item">
                <div>
                    <div class="history-title">${escapeHTML(title)}</div>
                    <div class="history-date">${formatTimestamp(item.createdAt)}</div>
                    ${progressText}
                </div>
                <div class="history-amount">+ ${formatCurrency(item.amount)}</div>
            </div>
        `;
    }).join("");
}

async function supportCampaign(campaignId) {
    if (currentRole !== "CONTRIBUTOR") return;
    const c = campaigns.find((i) => i.id === campaignId);
    if (!c) {
        showToast("Campaign not found.", "error");
        return;
    }
    const { target, collected } = progressOf(c);
    const remaining = Math.round((target - collected) * 100) / 100;
    if (statusOf(c) !== "APPROVED" || remaining <= 0) {
        showToast("This campaign is not accepting contributions.", "error");
        return;
    }

    const value = await openDialog({
        title: "Demo Contribution",
        message: `"${c.title}". Remaining goal: ${formatCurrency(remaining)}. This records a simulated demo contribution for testing; no actual money transfer is made.`,
        field: "number",
        label: "Simulated Amount",
        placeholder: "Enter amount",
        confirmText: "Record Demo Contribution",
        validate: (text) => {
            if (!/^\d+(\.\d{1,2})?$/.test(text)) return "Enter a positive amount with at most 2 decimals.";
            const amount = Number(text);
            if (amount <= 0) return "Amount must be greater than zero.";
            if (amount > remaining) return `Maximum contribution is ${formatCurrency(remaining)}.`;
            return "";
        }
    });

    if (value === null) return;

    await runAction(
        "Recording demo contribution...",
        () => apiRequest(`/api/campaigns/${campaignId}/contributions`, {
            method: "POST",
            body: { amount: Number(value) }
        }),
        "Demo contribution recorded. Thank you!"
    );
}

/* --- CREATOR ACTIONS & RENDERING --- */
function renderCreatorCampaigns() {
    const container = $("creatorCampaigns");
    const searchText = $("creatorCampaignSearch").value.trim().toLowerCase();
    const own = sortCampaigns(campaigns.filter((c) => c.creatorId === currentUser.uid && matchesSearch(c, searchText, ["title", "description"])));

    if (own.length === 0) {
        container.innerHTML = `<div class="empty-message">${searchText ? "No campaigns match your search." : "You haven't created any campaigns yet. Use Create Campaign to start one."}</div>`;
        return;
    }
    container.innerHTML = own.map(renderCreatorCard).join("");
}

function renderCreatorCard(c) {
    const { target, collected, percentage } = progressOf(c);
    const status = statusOf(c);
    const messages = {
        PENDING: ["pending", "Waiting for admin approval."],
        APPROVED: ["approved", "Approved. Your campaign is live and accepting contributions."],
        COMPLETED: ["completed", "Funding target reached."],
        REJECTED: ["rejected", c.rejectionReason ? `Rejected: ${c.rejectionReason}` : "Rejected by the admin."]
    };
    const [messageClass, messageText] = messages[status] || messages.PENDING;

    return `
        <article class="campaign-card">
            <div class="campaign-card-content">
                <span class="creator-card-status ${getStatusClass(status)}">${escapeHTML(status)}</span>
                <h3 class="campaign-title">${escapeHTML(c.title || "Untitled campaign")}</h3>
                <p class="campaign-description">${escapeHTML(c.description || "No description available.")}</p>
                <div class="campaign-amount-row">
                    <div>
                        <span class="campaign-raised-amount">${formatCurrency(collected)}</span>
                        <span class="campaign-raised-label">raised</span>
                    </div>
                    <strong class="campaign-percentage">${percentage}%</strong>
                </div>
                ${progressBar(percentage)}
                <div class="campaign-goal-line">
                    <span>Goal</span>
                    <strong>${formatCurrency(target)}</strong>
                </div>
                <div class="creator-status-message ${messageClass}">${escapeHTML(messageText)}</div>
            </div>
        </article>
    `;
}

function showCampaignMessage(message, isError = false) {
    campaignMessage.textContent = message;
    campaignMessage.className = isError ? "form-message error-form-message" : "form-message success-message";
}

campaignForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (currentRole !== "CREATOR" || !currentUser || busy) return;

    const title = $("campaignTitle").value.trim();
    const description = $("campaignDescription").value.trim();
    const targetText = $("campaignTarget").value.trim();
    const target = Number(targetText);

    campaignMessage.className = "form-message";
    campaignMessage.textContent = "";

    if (!title || !description || !targetText || !Number.isFinite(target) || target <= 0) {
        showCampaignMessage("Please provide a valid title, description, and positive target amount.", true);
        return;
    }
    if (!/^\d+(\.\d{1,2})?$/.test(targetText)) {
        showCampaignMessage("Target amount can have at most 2 decimal places.", true);
        return;
    }

    setButtonLoading(createCampaignButton, true, "Submitting...");
    const created = await runAction(
        "Submitting your campaign...",
        () => apiRequest("/api/campaigns", {
            method: "POST",
            body: { title, description, targetAmount: target }
        })
    );
    setButtonLoading(createCampaignButton, false);

    if (created) {
        campaignForm.reset();
        showCampaignMessage("Campaign submitted. It is now waiting for admin approval and appears in My Campaigns.", false);
    } else {
        showCampaignMessage("The campaign could not be submitted. Check your details and try again.", true);
    }
});

$("creatorCampaignSearch").addEventListener("input", () => { if (currentRole === "CREATOR") renderCreatorCampaigns(); });
$("contributorCampaignSearch").addEventListener("input", () => { if (currentRole === "CONTRIBUTOR") renderContributorCampaigns(); });

/* --- ADMIN ACTIONS & RENDERING --- */
function renderAdminCampaigns() {
    const container = $("adminCampaigns");
    if (campaigns.length === 0) {
        container.innerHTML = `<tr><td colspan="4" class="table-loading">No campaigns found.</td></tr>`;
        return;
    }
    container.innerHTML = sortCampaigns(campaigns, true).map((c) => {
        const status = statusOf(c);
        const { target, collected, percentage } = progressOf(c);
        const actions = status === "PENDING"
            ? `
                <div class="admin-actions">
                    <button type="button" class="approve-button" data-action="approve" data-id="${escapeHTML(c.id)}">✓ Approve</button>
                    <button type="button" class="reject-button" data-action="reject" data-id="${escapeHTML(c.id)}">✕ Reject</button>
                </div>
            `
            : '<span class="muted">No action needed</span>';
        const reason = status === "REJECTED" && c.rejectionReason ? `<div class="admin-reason">Reason: ${escapeHTML(c.rejectionReason)}</div>` : "";

        return `
            <tr>
                <td>
                    <strong class="admin-campaign-title">${escapeHTML(c.title || "Untitled campaign")}</strong>
                    <div class="admin-campaign-desc">${escapeHTML(c.description || "")}</div>
                    <div class="admin-campaign-meta">Creator: ${escapeHTML(c.creatorName || "Unknown")}</div>
                    ${reason}
                </td>
                <td>
                    <div>${formatCurrency(collected)} of ${formatCurrency(target)}</div>
                    <div class="mini-progress"><div style="width: ${percentage}%"></div></div>
                    <div class="admin-campaign-meta">${percentage}%</div>
                </td>
                <td><span class="creator-card-status ${getStatusClass(status)}">${escapeHTML(status)}</span></td>
                <td>${actions}</td>
            </tr>
        `;
    }).join("");
}

async function approveCampaign(campaignId) {
    if (currentRole !== "ADMIN") return;
    const c = campaigns.find((i) => i.id === campaignId);
    if (!c || statusOf(c) !== "PENDING") {
        showToast("Only pending campaigns can be approved.", "error");
        return;
    }
    const confirmed = await openDialog({
        title: "Approve campaign",
        message: `Approve "${c.title}"? It will become visible to contributors.`,
        confirmText: "Approve"
    });
    if (!confirmed) return;

    await runAction(
        "Approving campaign...",
        () => apiRequest(`/api/campaigns/${campaignId}/approve`, { method: "POST" }),
        "Campaign approved."
    );
}

async function rejectCampaign(campaignId) {
    if (currentRole !== "ADMIN") return;
    const c = campaigns.find((i) => i.id === campaignId);
    if (!c || statusOf(c) !== "PENDING") {
        showToast("Only pending campaigns can be rejected.", "error");
        return;
    }
    const reason = await openDialog({
        title: "Reject campaign",
        message: `Tell the creator why "${c.title}" is being rejected.`,
        field: "textarea",
        label: "Rejection reason",
        placeholder: "For example: the description does not explain how the funds will be used.",
        confirmText: "Reject campaign",
        validate: (t) => (!t ? "A rejection reason is required." : t.length > 500 ? "Please keep under 500 characters." : "")
    });
    if (reason === null) return;

    await runAction(
        "Rejecting campaign...",
        () => apiRequest(`/api/campaigns/${campaignId}/reject`, {
            method: "POST",
            body: { reason }
        }),
        "Campaign rejected."
    );
}

function getRoleBadgeClass(role) {
    switch (String(role || "").toUpperCase()) {
        case "ADMIN": return "role-admin";
        case "CREATOR": return "role-creator";
        case "CONTRIBUTOR": return "role-contributor";
        default: return "role-unknown";
    }
}

function renderUsers() {
    const container = $("usersTableBody");
    if (users.length === 0) {
        container.innerHTML = `<tr><td colspan="4" class="table-loading">No users found.</td></tr>`;
        return;
    }
    container.innerHTML = [...users]
        .sort((a, b) => String(a.name || a.email || "").localeCompare(String(b.name || b.email || "")))
        .map((user) => {
            const role = String(user.ROLE || "UNKNOWN").toUpperCase();
            const isSelf = user.id === currentUser.uid;
            return `
                <tr>
                    <td>${escapeHTML(user.name || "Unnamed user")} ${isSelf ? '<span class="current-user-label">(you)</span>' : ""}</td>
                    <td>${escapeHTML(user.email || "")}</td>
                    <td><span class="role-badge ${getRoleBadgeClass(role)}">${escapeHTML(role)}</span></td>
                    <td>${isSelf ? '<span class="muted">Cannot edit own role</span>' : `<button type="button" class="edit-role-button" data-action="edit-role" data-id="${escapeHTML(user.id)}">Edit role</button>`}</td>
                </tr>
            `;
        }).join("");
}

function openRoleEditor(userId) {
    if (currentRole !== "ADMIN") return;
    const selected = users.find((u) => u.id === userId);
    if (!selected) {
        showToast("User not found.", "error");
        return;
    }
    editingUserId = userId;
    $("editUserName").textContent = selected.name || selected.email || "User";
    editUserRole.value = VALID_ROLES.includes(String(selected.ROLE || "").toUpperCase()) ? String(selected.ROLE).toUpperCase() : "CONTRIBUTOR";
    roleModal.classList.remove("hidden");
    editUserRole.focus();
}

function closeRoleEditor() {
    editingUserId = null;
    roleModal.classList.add("hidden");
}

$("closeRoleModal").addEventListener("click", closeRoleEditor);
$("cancelRoleEdit").addEventListener("click", closeRoleEditor);
roleModal.addEventListener("click", (e) => { if (e.target === roleModal) closeRoleEditor(); });
document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !roleModal.classList.contains("hidden")) closeRoleEditor(); });

saveRoleButton.addEventListener("click", async () => {
    if (currentRole !== "ADMIN" || !editingUserId || busy) return;
    const newRole = editUserRole.value;
    const selected = users.find((u) => u.id === editingUserId);
    if (!VALID_ROLES.includes(newRole) || !selected) {
        showToast("Invalid role selection.", "error");
        return;
    }
    if (String(selected.ROLE || "").toUpperCase() === newRole) {
        closeRoleEditor();
        return;
    }
    const targetId = editingUserId;
    setButtonLoading(saveRoleButton, true, "Saving...");
    const saved = await runAction(
        "Updating user role...",
        () => apiRequest(`/api/users/${targetId}/role`, {
            method: "PATCH",
            body: { role: newRole }
        }),
        `Role changed to ${getRoleLabel(newRole)}.`
    );
    setButtonLoading(saveRoleButton, false);
    if (saved) closeRoleEditor();
});

/* --- CLICK DELEGATION --- */
document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-action]");
    if (!button || button.disabled) return;
    const { action, id } = button.dataset;
    if (action === "support") supportCampaign(id);
    else if (action === "approve") approveCampaign(id);
    else if (action === "reject") rejectCampaign(id);
    else if (action === "edit-role") openRoleEditor(id);
});
