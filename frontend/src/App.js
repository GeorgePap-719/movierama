import React, {useEffect, useState} from 'react';
import './App.css';

function App() {

  const [movies, setMovies] = useState([]);
  const [showSignUpModal, setShowSignUpModal] = useState(false);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showSubmitMovieButton, setShowSubmitMovieButton] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [userId, setUserId] = useState(-1);
  const [token, setToken] = useState('');
  // Movie fields.
  const [movieTitle, setNewMovieTitle] = useState('');
  const [movieDescription, setNewMovieDescription] = useState('');

  useEffect(() => {
    fetchMoviesFromBackend();
  }, []);

  const fetchMoviesFromBackend = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/movies', {});
      if (!response.ok) {
        throw new Error('Failed to fetch movies');
      }
      const data = await response.json();
      setMovies(data);
    } catch (error) {
      console.error('Error fetching movies:', error);
    }
  };

  const handleSignUpClick = () => {
    setShowSignUpModal(true);
  };

  const handleLoginClick = () => {
    setShowLoginModal(true);
  };

  const closeSignUpModal = () => {
    setShowSignUpModal(false);
  };

  const closeLoginModal = () => {
    setShowLoginModal(false);
  };

  const handleSignUpSubmit = async (event) => {
    event.preventDefault();
    try {
      const response = await fetch('http://localhost:8080/api/auth/register', {
        method: "POST",
        headers: {
          "Accept": "application/json",
          "Content-Type": "application/json"
        },
        body: JSON.stringify({name: username, password})
      });
      if (!response.ok) {
        const error = response.toString()
        console.log(error)
        throw new Error('Failed to register user');
      }
      console.log('User registered successfully');
      closeSignUpModal();
    } catch (error) {
      console.error('Error registering user:', error);
    }
  };

  const handleLoginSubmit = async (event) => {
    event.preventDefault();
    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({name: username, password}),
      });
      if (!response.ok) {
        throw new Error('Failed to login');
      }
      const data = await response.json();
      setToken(data.token);
      console.log('User logged in successfully');
      setLoggedIn(true);
      setUserId(data.id)
      closeLoginModal();
    } catch (error) {
      console.error('Error logging in:', error);
    }
  };

  let movie = {
    title: movieTitle,
    description: movieDescription,
    user_id: userId
  };

  const handleNewMovieSubmit = async (event) => {
    event.preventDefault();
    try {
      const response = await fetch('http://localhost:8080/api/movies', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify(movie),
      });
      if (!response.ok) {
        console.log(await response.json())
        throw new Error(`Failed post new movie`);
      }
      const data = await response.json();
      console.log("Added new movie successfully.")
      // Refresh movies.
      await fetchMoviesFromBackend()
    } catch (error) {
      console.error('Error adding new movie:', error);
    }
  };

  return (
      <div className="App">
        <header className="App-header">
          <div className="header-buttons">
            {!loggedIn && <button onClick={handleSignUpClick}>Sign Up</button>}
            {!loggedIn && <button onClick={handleLoginClick}>Login</button>}
            {loggedIn && <span>Welcome, {username}</span>}
            {loggedIn && <button
                onClick={() => setShowSubmitMovieButton(true)}> New
              movie </button>}
          </div>
          <h1>Browse Movies</h1>
        </header>
        <main>
          <div className="movie-list">
            {movies.map(movie => (
                <div key={movie.id} className="movie">
                  <h2 className="movie-title">{movie.title}</h2>
                  <div className="movie-details">
                    <p><strong>Posted by:</strong> {movie.posted_by_user}</p>
                    <p><strong>Description:</strong> {movie.description}</p>
                    <p><strong>Likes:</strong> {movie.likes}</p>
                    <p><strong>Hates:</strong> {movie.hates}</p>
                    <p><strong>Release Date:</strong> {new Date(
                        movie.date).toLocaleString()}</p>
                  </div>
                </div>
            ))}
          </div>
        </main>
        {showSignUpModal && (
            <div className="modal">
              <div className="modal-content">
                <span className="close"
                      onClick={closeSignUpModal}>&times;</span>
                <h2>Sign Up</h2>
                <form onSubmit={handleSignUpSubmit}>
                  <div className="form-group">
                    <label htmlFor="username">Username:</label>
                    <input type="text" id="username" value={username}
                           onChange={(e) => setUsername(e.target.value)}/>
                  </div>
                  <div className="form-group">
                    <label htmlFor="password">Password:</label>
                    <input type="password" id="password" value={password}
                           onChange={(e) => setPassword(e.target.value)}/>
                  </div>
                  <button type="submit">Sign Up</button>
                </form>
              </div>
            </div>
        )}
        {showLoginModal && (
            <div className="modal">
              <div className="modal-content">
                <span className="close" onClick={closeLoginModal}>&times;</span>
                <h2>Login</h2>
                <form onSubmit={handleLoginSubmit}>
                  <div className="form-group">
                    <label htmlFor="username">Username:</label>
                    <input type="text" id="username" value={username}
                           onChange={(e) => setUsername(e.target.value)}/>
                  </div>
                  <div className="form-group">
                    <label htmlFor="password">Password:</label>
                    <input type="password" id="password" value={password}
                           onChange={(e) => setPassword(e.target.value)}/>
                  </div>
                  <button type="submit">Login</button>
                </form>
              </div>
            </div>
        )}
        {showSubmitMovieButton && (
            <div className="modal">
              <div className="modal-content">
                <span className="close" onClick={() => setShowSubmitMovieButton(
                    false)}>&times;</span>
                <h2>New Movie</h2>
                <form onSubmit={handleNewMovieSubmit}>
                  <div className="form-group">
                    <label htmlFor="newMovieTitle">Title:</label>
                    <input type="text" id="newMovieTitle" value={movieTitle}
                           onChange={(e) => setNewMovieTitle(e.target.value)}/>
                  </div>
                  <div className="form-group">
                    <label htmlFor="newMovieDescription">Description:</label>
                    <textarea id="newMovieDescription" value={movieDescription}
                              onChange={(e) => setNewMovieDescription(
                                  e.target.value)}/>
                  </div>
                  <button type="submit">Add Movie</button>
                </form>
              </div>
            </div>
        )}
      </div>
  );
}

export default App;
