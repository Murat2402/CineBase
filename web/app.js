// Durum değişkenleri
let token = localStorage.getItem('film_token');
let filmler = [];
let filtreliFilmler = [];
let fetchTimer = null;

// DOM Elementleri
const els = {
  loginScreen: document.getElementById('login-screen'),
  appScreen: document.getElementById('app-screen'),
  loginForm: document.getElementById('login-form'),
  loginError: document.getElementById('login-error'),
  btnLogout: document.getElementById('btn-logout'),
  
  grid: document.getElementById('films-grid'),
  emptyState: document.getElementById('empty-state'),
  searchInput: document.getElementById('search-input'),
  filterType: document.getElementById('filter-type'),
  filterDecade: document.getElementById('filter-decade'),
  sortType: document.getElementById('sort-type'),
  genreFilter: document.getElementById('genre-filter'),
  
  btnFetch: document.getElementById('btn-fetch-api'),
  statusBanner: document.getElementById('status-banner'),
  statusText: document.getElementById('status-text'),
  
  dashTotal: document.getElementById('dash-total'),
  dashFavs: document.getElementById('dash-favs'),
  
  editModal: document.getElementById('edit-modal'),
  editForm: document.getElementById('edit-form'),
  editError: document.getElementById('edit-error'),
  btnCloseModal: document.getElementById('btn-close-modal'),
  btnCancelModal: document.getElementById('btn-cancel-modal')
};

// Başlangıç Kontrolü
init();

function init() {
  if (token) {
    gosterEkran(els.appScreen);
    verileriYukle();
  } else {
    gosterEkran(els.loginScreen);
  }
}

// Olay Dinleyicileri
els.loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const user = document.getElementById('username').value;
  const pass = document.getElementById('password').value;
  
  const btn = document.getElementById('btn-login');
  const orgHtml = btn.innerHTML;
  btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Bekleyin...';
  
  try {
    const r = await apiPost('/api/login', { user, pass }, false);
    token = r.token;
    localStorage.setItem('film_token', token);
    els.loginForm.reset();
    els.loginError.innerText = '';
    gosterEkran(els.appScreen);
    verileriYukle();
  } catch (err) {
    els.loginError.innerText = err.message;
  } finally {
    btn.innerHTML = orgHtml;
  }
});

els.btnLogout.addEventListener('click', async () => {
  if(token) { try { await apiPost('/api/logout', {}); } catch(e){} }
  token = null;
  localStorage.removeItem('film_token');
  gosterEkran(els.loginScreen);
});

els.searchInput.addEventListener('input', filtreUygula);
els.filterType.addEventListener('change', filtreUygula);
els.filterDecade.addEventListener('change', filtreUygula);
els.sortType.addEventListener('change', filtreUygula);
els.genreFilter.addEventListener('input', filtreUygula);

els.btnFetch.addEventListener('click', async () => {
  try {
    const r = await apiPost('/api/fetch', {});
    if (r.started) {
      els.statusBanner.classList.remove('hidden');
      if (!fetchTimer) fetchTimer = setInterval(durumKontrol, 2000);
    }
  } catch(e) { alert("Hata: " + e.message); }
});

els.btnCloseModal.addEventListener('click', () => els.editModal.classList.add('hidden'));
els.btnCancelModal.addEventListener('click', () => els.editModal.classList.add('hidden'));

els.editForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  els.editError.innerText = '';
  const data = {
    title: document.getElementById('edit-old-title').value,
    newTitle: document.getElementById('edit-title').value,
    year: document.getElementById('edit-year').value,
    imdb: document.getElementById('edit-imdb').value,
    genre: document.getElementById('edit-genre').value,
    runtime: document.getElementById('edit-runtime').value,
    director: document.getElementById('edit-director').value
  };
  
  try {
    await apiPost('/api/films/update', data);
    els.editModal.classList.add('hidden');
    verileriYukle();
  } catch(err) { els.editError.innerText = err.message; }
});

// --- API İşlemleri ---

async function verileriYukle() {
  try {
    const response = await fetch('/api/films', { headers: { 'X-Token': token } });
    if (response.status === 401) throw new Error("Oturum süresi doldu.");
    if (!response.ok) throw new Error("Veriler alınamadı.");
    
    filmler = await response.json();
    guncelleDashboard();
    filtreUygula();
  } catch (err) {
    if(err.message === "Oturum süresi doldu.") els.btnLogout.click();
    console.error(err);
  }
}

async function durumKontrol() {
  try {
    const response = await fetch('/api/fetch/status', { headers: { 'X-Token': token } });
    if(response.status !== 200) return;
    const data = await response.json();
    
    if (data.running) {
      els.statusText.innerText = `API'den veriler çekiliyor: ${data.done} / ${data.total}`;
    } else {
      clearInterval(fetchTimer); fetchTimer = null;
      els.statusBanner.classList.add('hidden');
      if (data.message) alert(data.message);
      verileriYukle();
    }
  } catch(e) {}
}

async function apiPost(url, data, auth = true) {
  const body = new URLSearchParams(data).toString();
  const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
  if (auth) headers['X-Token'] = token;
  
  const response = await fetch(url, { method: 'POST', headers, body });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401 && auth) { els.btnLogout.click(); throw new Error("Oturum kapandı."); }
    throw new Error(json.error || 'İşlem başarısız.');
  }
  return json;
}

// --- Yardımcı Fonksiyonlar ---
function getSüreDk(runtimeStr) {
  if (!runtimeStr) return 0;
  const match = runtimeStr.match(/(\d+)/);
  return match ? parseInt(match[1]) : 0;
}

function guncelleDashboard() {
  els.dashTotal.innerText = filmler.length;
  els.dashFavs.innerText = filmler.filter(f => f.favorite).length;
}

// --- UI / Mantık ---

function filtreUygula() {
  const q = els.searchInput.value.toLowerCase().trim();
  const tip = els.filterType.value;
  const decade = els.filterDecade.value;
  const tur = els.genreFilter.value.toLowerCase().trim();
  const sirala = els.sortType.value;
  
  filtreliFilmler = filmler.filter(f => {
    // Arama
    if (q && !f.title.toLowerCase().includes(q) && !f.director.toLowerCase().includes(q)) return false;
    // Tür
    if (tur && !f.genre.toLowerCase().includes(tur)) return false;
    
    // Tip Filtresi
    if (tip === 'imdb8') {
      const imdb = parseFloat(f.imdb);
      if (isNaN(imdb) || imdb < 8.0) return false;
    }
    if (tip === 'favorites' && !f.favorite) return false;
    
    const hasPoster = f.poster && f.poster !== 'N/A' && f.poster.startsWith('http');
    if (tip === 'hasPoster' && !hasPoster) return false;
    if (tip === 'noPoster' && hasPoster) return false;

    // Yıl Aralığı Filtresi
    if (decade !== 'all') {
      const year = parseInt(f.year);
      if (isNaN(year)) return false;
      if (decade === 'old' && year >= 1990) return false;
      if (decade !== 'old') {
        const d = parseInt(decade);
        if (year < d || year > d + 9) return false;
      }
    }
    
    return true;
  });

  // Sıralama
  filtreliFilmler.sort((a, b) => {
    if (sirala === 'yearDesc') return parseInt(b.year || 0) - parseInt(a.year || 0);
    if (sirala === 'yearAsc') return parseInt(a.year || 0) - parseInt(b.year || 0);
    if (sirala === 'imdbDesc') return parseFloat(b.imdb || 0) - parseFloat(a.imdb || 0);
    if (sirala === 'imdbAsc') return parseFloat(a.imdb || 0) - parseFloat(b.imdb || 0);
    if (sirala === 'titleAsc') return a.title.localeCompare(b.title);
    if (sirala === 'runtimeDesc') return getSüreDk(b.runtime) - getSüreDk(a.runtime);
    if (sirala === 'runtimeAsc') return getSüreDk(a.runtime) - getSüreDk(b.runtime);
    return 0;
  });
  
  renderGrid();
}

function renderGrid() {
  els.grid.innerHTML = '';
  
  if (filtreliFilmler.length === 0) {
    els.emptyState.classList.remove('hidden');
  } else {
    els.emptyState.classList.add('hidden');
    
    filtreliFilmler.forEach((f) => {
      const card = document.createElement('div');
      card.className = 'film-card'; // animasyonu sildik, hover border rengi CSS'te var
      
      const posterSrc = (f.poster && f.poster !== 'N/A' && f.poster.startsWith('http')) 
          ? f.poster 
          : 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&q=80&w=400&h=600';
          
      const heartClass = f.favorite ? 'bi-heart-fill active' : 'bi-heart';
      
      card.innerHTML = `
        <div class="poster-box">
          <img src="${posterSrc}" class="poster-img" alt="${f.title}" loading="lazy" onerror="this.src='https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&q=80&w=400&h=600'">
          <div class="badge-imdb"><i class="bi bi-star-fill"></i> ${f.imdb}</div>
          <button class="btn-fav ${f.favorite ? 'active' : ''}" data-title="${f.title}">
            <i class="bi ${heartClass}"></i>
          </button>
        </div>
        
        <div class="film-info">
          <div class="film-title">${f.title} <span class="film-meta">(${f.year})</span></div>
          
          <div class="film-details">
            <span class="film-genre">${f.genre.split(',')[0] || 'Tür Yok'}</span>
            <span><i class="bi bi-clock"></i> ${f.runtime || '-'}</span>
          </div>
          
          <div class="film-director"><i class="bi bi-person-video"></i> ${f.director || '-'}</div>
          
          <div class="film-actions">
            <button class="btn btn-secondary btn-sm btn-edit" data-title="${f.title}"><i class="bi bi-pencil"></i> Düzenle</button>
            <button class="btn btn-outline btn-sm btn-delete" data-title="${f.title}"><i class="bi bi-trash text-danger"></i> Sil</button>
          </div>
        </div>
      `;
      els.grid.appendChild(card);
    });
    
    document.querySelectorAll('.btn-fav').forEach(btn => btn.addEventListener('click', islemFav));
    document.querySelectorAll('.btn-edit').forEach(btn => btn.addEventListener('click', islemDuzenleSec));
    document.querySelectorAll('.btn-delete').forEach(btn => btn.addEventListener('click', islemSil));
  }
}

function gosterEkran(ekran) {
  els.loginScreen.classList.add('hidden');
  els.appScreen.classList.add('hidden');
  ekran.classList.remove('hidden');
}

// --- Eylemler ---

async function islemFav(e) {
  const btn = e.currentTarget;
  const title = btn.getAttribute('data-title');
  const film = filmler.find(f => f.title === title);
  if(!film) return;
  
  const icon = btn.querySelector('i');
  icon.className = 'bi bi-hourglass-split';
  
  const url = film.favorite ? '/api/favorites/remove' : '/api/favorites/add';
  try {
    await apiPost(url, { title });
    film.favorite = !film.favorite;
    guncelleDashboard();
    renderGrid();
  } catch(err) {
    alert("Hata: " + err.message);
    renderGrid();
  }
}

function islemDuzenleSec(e) {
  const title = e.currentTarget.getAttribute('data-title');
  const film = filmler.find(f => f.title === title);
  if(!film) return;
  
  document.getElementById('edit-old-title').value = film.title;
  document.getElementById('edit-title').value = film.title;
  document.getElementById('edit-year').value = film.year;
  document.getElementById('edit-imdb').value = film.imdb;
  document.getElementById('edit-genre').value = film.genre;
  document.getElementById('edit-runtime').value = film.runtime;
  document.getElementById('edit-director').value = film.director;
  
  els.editError.innerText = '';
  els.editModal.classList.remove('hidden');
}

async function islemSil(e) {
  const title = e.currentTarget.getAttribute('data-title');
  if(!confirm(`"${title}" filmini silmek istediğinize emin misiniz?`)) return;
  
  try {
    await apiPost('/api/films/delete', { title });
    verileriYukle();
  } catch(err) {
    alert("Hata: " + err.message);
  }
}
