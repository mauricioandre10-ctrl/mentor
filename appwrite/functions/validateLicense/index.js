const sdk = require('node-appwrite');

module.exports = async function (req, res) {
  const client = new sdk.Client();
  const databases = new sdk.Databases(client);

  if (
    !process.env.APPWRITE_FUNCTION_ENDPOINT ||
    !process.env.APPWRITE_FUNCTION_API_KEY
  ) {
    console.warn('Environment variables are not set.');
    res.json({ error: 'Environment variables are not set.' }, 500);
    return;
  }

  client
    .setEndpoint(process.env.APPWRITE_FUNCTION_ENDPOINT)
    .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
    .setKey(process.env.APPWRITE_FUNCTION_API_KEY);

  const userId = req.headers['x-appwrite-user-id'];

  if (!userId) {
    res.json({ error: 'User not authenticated.' }, 401);
    return;
  }

  try {
    const response = await databases.listDocuments(
        'main', // databaseId
        'licenses', // collectionId
        [sdk.Query.equal('userId', userId)]
    );

    if (response.documents.length === 0) {
        res.json({ status: 'NOT_FOUND', message: 'No license found for this user.' }, 404);
        return;
    }

    const license = response.documents[0];
    const expirationDate = new Date(license.expirationDate);
    const now = new Date();

    if (expirationDate > now) {
        res.json({ status: 'ACTIVE', expires: license.expirationDate });
    } else {
        res.json({ status: 'EXPIRED', expires: license.expirationDate });
    }

  } catch (error) {
    console.error(error);
    res.json({ error: 'An error occurred while validating the license.' }, 500);
  }
};
